package edge

import com.netflix.client.ClientException
import feign.FeignException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets

@Component
class EdgeServiceHandler(private val transactionsClient: TransactionsClient,
                         private val accountsClient: AccountsClient,
                         private val paymentsClient: PaymentsClient) {

    fun debit(request: ServerRequest): Mono<ServerResponse> =
            request.bodyToMono(Debit::class.java)
                    .map { (accountId, amount) ->
                        val transactionId = transactionsClient.begin().id
                        accountsClient.createDebitCommand(AccountsClient.CreateDebitCommand(accountId, transactionId, amount))
                        transactionsClient.commit(transactionId)
                        accountsClient.applyCommands(listOf(AccountsClient.ApplyCommand(accountId, transactionId, amount)))
                    }
                    .flatMap { ServerResponse.ok().build() }
                    .handleErrors()

    fun credit(request: ServerRequest): Mono<ServerResponse> =
            request.bodyToMono(Credit::class.java)
                    .map { (accountId, amount) ->
                        val transactionId = transactionsClient.begin().id
                        accountsClient.createCreditCommand(AccountsClient.CreateCreditCommand(accountId, transactionId))
                        transactionsClient.commit(transactionId)
                        accountsClient.applyCommands(listOf(AccountsClient.ApplyCommand(accountId, transactionId, amount)))
                    }
                    .flatMap { ServerResponse.ok().build() }
                    .handleErrors()

    fun transfer(request: ServerRequest): Mono<ServerResponse> =
            request.bodyToMono(TransferMoney::class.java)
                    .map { (fromAccount, toAccount, amount) ->
                        val transactionId = transactionsClient.begin().id
                        accountsClient.createDebitCommand(AccountsClient.CreateDebitCommand(fromAccount, transactionId, amount))
                        accountsClient.createCreditCommand(AccountsClient.CreateCreditCommand(toAccount, transactionId))
                        paymentsClient.create(PaymentsClient.CreatePayment(transactionId, fromAccount, toAccount, amount))
                        transactionsClient.commit(transactionId)
                        accountsClient.applyCommands(listOf(
                                AccountsClient.ApplyCommand(fromAccount, transactionId, amount),
                                AccountsClient.ApplyCommand(toAccount, transactionId, amount)
                        ))
                        paymentsClient.resolve(transactionId)
                    }
                    .flatMap { ServerResponse.ok().build() }
                    .handleErrors()
}

fun Mono<ServerResponse>.handleErrors(): Mono<ServerResponse> = this
        .onErrorResume({ it is RuntimeException && it.cause is ClientException }) {
            ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).syncBody(it.cause!!.message ?: "")
        }
        .onErrorResume(FeignException::class.java) {
            val status = HttpStatus.resolve(it.status()) ?: HttpStatus.SERVICE_UNAVAILABLE
            ServerResponse.status(status).syncBody(it.content()?.toString(StandardCharsets.UTF_8) ?: "")
        }
