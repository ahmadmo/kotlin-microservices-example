package transaction

import kotlinx.coroutines.*
import mu.KotlinLogging
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct
import javax.persistence.EntityNotFoundException
import kotlin.coroutines.CoroutineContext

@Component
class TransactionWatcher(
        private val service: TransactionService,
        private val props: TransactionProperties
) : CoroutineScope {

    private val logger = KotlinLogging.logger {}

    private val scheduledTransactionMap = ConcurrentHashMap<Long, Unit>()

    private lateinit var job: Job

    override val coroutineContext: CoroutineContext
        get() = CoroutineName("transaction-watcher") + Dispatchers.IO + job

    @PostConstruct
    private fun init() {
        job = Job()
        for (transaction in service.findAllInProgress()) {
            watch(transaction)
        }
    }

    fun watch(transaction: Transaction) {
        check(transaction.state == TransactionState.IN_PROGRESS)
        scheduledTransactionMap.computeIfAbsent(transaction.id) {
            logger.debug { "watch [id = ${transaction.id}]" }
            val abortionTime = transaction.createdAt.plus(props.timeout)
            val delay = Duration.between(Instant.now(), abortionTime)
            schedule(transaction.id, delay)
        }
    }

    private fun schedule(id: Long, delay: Duration) {
        launch {
            kotlinx.coroutines.delay(delay.toMillis())
            abort(id)
        }
    }

    private fun abort(id: Long) {
        if (!scheduledTransactionMap.containsKey(id)) return
        try {
            service.changeState(id, TransactionState.ABORTED)
            logger.warn { "aborted [id = $id]" }
            unwatch(id)
        } catch (e: Throwable) {
            handlerError(e, id)
        }
    }

    private fun handlerError(cause: Throwable, id: Long) = when (cause) {
        is EntityNotFoundException,
        is FinishedTransactionException,
        is OptimisticLockingFailureException ->
            unwatch(id)
        else -> {
            logger.error(cause) {
                "failed to abort transaction [id = $id]"
            }
            reschedule(id)
        }
    }

    private fun reschedule(id: Long) {
        scheduledTransactionMap.computeIfPresent(id) { _, _ ->
            schedule(id, props.timeout)
        }
    }

    fun unwatch(id: Long) {
        scheduledTransactionMap.remove(id)?.also {
            logger.debug { "unwatch [id = $id]" }
        }
    }
}
