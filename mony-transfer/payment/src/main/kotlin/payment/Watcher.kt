package payment

import kotlinx.coroutines.*
import mu.KotlinLogging
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import javax.annotation.PostConstruct
import javax.persistence.EntityNotFoundException
import kotlin.coroutines.CoroutineContext

@Component
class PaymentWatcher(
        private val service: PaymentService,
        private val props: WatcherProperties,
        private val transactionsClient: TransactionsClient
) : CoroutineScope {

    private val logger = KotlinLogging.logger {}

    private lateinit var job: Job

    private val queuedTransactionMap = ConcurrentHashMap<Long, Unit>()
    private val pendingQueue = LinkedBlockingQueue<Long>()

    override val coroutineContext: CoroutineContext
        get() = CoroutineName("payment-watcher") + Dispatchers.IO + job

    @PostConstruct
    private fun init() {
        job = Job()
        for (payment in service.findAllPending()) {
            watch(payment.transactionId)
        }
        launch { resolveAll() }
    }

    fun watch(transactionId: Long) {
        queuedTransactionMap.computeIfAbsent(transactionId) {
            logger.debug { "watch [transactionId = $transactionId]" }
            enqueue(transactionId)
        }
    }

    private fun enqueue(transactionId: Long) {
        pendingQueue.put(transactionId)
    }

    private suspend fun resolveAll() {
        try {
            var remaining = emptySequence<Long>()
            while (pendingQueue.isNotEmpty()) {
                generateSequence(pendingQueue::poll)
                        .filter(queuedTransactionMap::containsKey)
                        .take(props.batchSize)
                        .toList()
                        .takeIf { it.isNotEmpty() }
                        ?.also { remaining += resolve(it) }
            }
            remaining.forEach(this::requeue)
        } finally {
            delay(props.linger.toMillis())
            resolveAll()
        }
    }

    private fun resolve(ids: List<Long>): Sequence<Long> = try {
        val transactions = transactionsClient.findAllById(ids)
        sequence {
            for (transaction in transactions) {
                if (transaction.state == TransactionsClient.TransactionState.IN_PROGRESS) {
                    yield(transaction.id)
                } else launch {
                    delay(props.period.toMillis())
                    resolve(transaction)
                }
            }
        }
    } catch (e: Throwable) {
        logger.error(e) { "transaction service error" }
        ids.asSequence()
    }

    private fun resolve(transaction: TransactionsClient.Transaction) {
        val transactionId = transaction.id
        if (!queuedTransactionMap.containsKey(transactionId)) return
        try {
            service.resolve(transaction)
            logger.info {
                "resolved [transactionId = $transactionId, state = ${transaction.state}]"
            }
            unwatch(transactionId)
        } catch (e: Throwable) {
            handlerError(e, transactionId)
        }
    }

    private fun handlerError(cause: Throwable, transactionId: Long) = when (cause) {
        is EntityNotFoundException,
        is DuplicateKeyException ->
            unwatch(transactionId)
        else -> {
            logger.error(cause) {
                "failed to resolve payment [transactionId = $transactionId]"
            }
            requeue(transactionId)
        }
    }

    private fun requeue(transactionId: Long) {
        queuedTransactionMap.computeIfPresent(transactionId) { _, _ ->
            enqueue(transactionId)
        }
    }

    fun unwatch(transactionId: Long) {
        queuedTransactionMap.remove(transactionId)?.also {
            logger.debug { "unwatch [transactionId = $transactionId]" }
        }
    }
}
