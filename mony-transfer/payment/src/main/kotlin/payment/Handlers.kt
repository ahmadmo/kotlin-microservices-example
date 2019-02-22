package payment

import com.netflix.client.ClientException
import feign.FeignException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.json
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets
import javax.persistence.EntityNotFoundException

@Component
class PaymentServiceHandler(private val service: PaymentService,
                            private val transactions: TransactionsClient) {

    fun create(request: ServerRequest): Mono<ServerResponse> = request
            .bodyToMono(CreatePayment::class.java)
            .map { (transactionId, fromAccount, toAccount, amount) ->
                service.create(transactionId, fromAccount, toAccount, amount)
            }
            .flatMap { ServerResponse.ok().build() }

    fun resolve(request: ServerRequest): Mono<ServerResponse> = Mono
            .fromCallable {
                val transactionId = request.pathVariable("id").toLong()
                val transactions = transactions.findByIds(listOf(transactionId))
                val transaction = transactions.firstOrNull() ?: throw EntityNotFoundException()
                if (transaction.state == TransactionsClient.TransactionState.IN_PROGRESS) {
                    throw TransactionNotFinishedException
                }
                service.resolve(transaction)
            }
            .flatMap { ServerResponse.ok().build() }
            .handleErrors()

    fun findAll(request: ServerRequest): Mono<ServerResponse> {
        val page = request.queryParam("page").map { it.toInt() }.orElse(0)
        val size = request.queryParam("size").map { Math.min(it.toInt(), 50) }.orElse(10)
        return when {
            page < 0 || size < 1 -> ServerResponse.badRequest().build()
            else -> Mono.fromCallable { service.findAll(page, size) }
                    .flatMap { payments -> ServerResponse.ok().json().syncBody(payments) }
        }
    }
}

fun Mono<ServerResponse>.handleErrors(): Mono<ServerResponse> = this
        .onErrorResume({ it is RuntimeException && it.cause is ClientException }) {
            ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).syncBody(it.cause!!.message ?: "")
        }
        .onErrorResume(FeignException::class.java) {
            val status = HttpStatus.resolve(it.status()) ?: HttpStatus.SERVICE_UNAVAILABLE
            ServerResponse.status(status).syncBody(it.content()?.toString(StandardCharsets.UTF_8) ?: "")
        }
        .onErrorResume(EntityNotFoundException::class.java) {
            ServerResponse.status(HttpStatus.NOT_FOUND).syncBody(it.message ?: "")
        }
        .onErrorResume(TransactionNotFinishedException::class.java) {
            ServerResponse.badRequest().syncBody("Transaction Not Finished")
        }
