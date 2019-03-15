package payment

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.json
import reactor.core.publisher.Mono
import javax.persistence.EntityNotFoundException

@Component
class PaymentServiceHandler(private val service: PaymentService,
                            private val watcher: PaymentWatcher,
                            private val transactionsClient: TransactionsClient,
                            private val exceptionMapper: ExceptionResponseStatusMapper) {

    fun create(request: ServerRequest): Mono<ServerResponse> = request
            .bodyToMono(CreatePayment::class.java)
            .map { (transactionId, fromAccount, toAccount, amount) ->
                service.create(transactionId, fromAccount, toAccount, amount)
                watcher.watch(transactionId)
            }
            .flatMap { ServerResponse.ok().build() }

    fun resolve(request: ServerRequest): Mono<ServerResponse> = Mono
            .fromCallable {
                val transactionId = request.pathVariable("id").toLong()
                val transactions = transactionsClient.findAllById(listOf(transactionId))
                val transaction = transactions.firstOrNull() ?: throw EntityNotFoundException()
                if (transaction.state == TransactionsClient.TransactionState.IN_PROGRESS) {
                    throw NotFinishedTransactionException
                }
                service.resolve(transaction)
                watcher.unwatch(transactionId)
            }
            .flatMap { ServerResponse.ok().build() }
            .onErrorMap(exceptionMapper::map)

    fun findAll(request: ServerRequest): Mono<ServerResponse> = Mono
            .fromCallable {
                val page = request.queryParam("page").map { Math.max(it.toInt(), 0) }.orElse(0)
                val size = request.queryParam("size").map { Math.max(Math.min(it.toInt(), 50), 0) }.orElse(10)
                service.findAll(page, size)
            }
            .flatMap { payments ->
                ServerResponse.ok().json().syncBody(payments.response())
            }
}
