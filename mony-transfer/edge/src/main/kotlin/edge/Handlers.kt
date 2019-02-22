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
class EdgeServiceHandler(private val transactions: TransactionsClient,
                         private val accounts: AccountsClient,
                         private val payments: PaymentsClient) {

    // TODO transaction error handling

    fun debit(request: ServerRequest): Mono<ServerResponse> = request
            .bodyToMono(Debit::class.java)
            .map { (accountId, amount) ->
                val transactionId = transactions.begin().id
                accounts.createDebitCommand(AccountsClient.CreateDebitCommand(accountId, transactionId, amount))
                payments.create(PaymentsClient.CreatePayment(transactionId, accountId, 0, amount))
                transactions.commit(transactionId)
                accounts.applyCommands(listOf(AccountsClient.ApplyCommand(accountId, transactionId, amount)))
                payments.resolve(transactionId)
            }
            .flatMap { ServerResponse.ok().build() }
            .handleErrors()

    fun credit(request: ServerRequest): Mono<ServerResponse> = request
            .bodyToMono(Credit::class.java)
            .map { (accountId, amount) ->
                val transactionId = transactions.begin().id
                accounts.createCreditCommand(AccountsClient.CreateCreditCommand(accountId, transactionId))
                payments.create(PaymentsClient.CreatePayment(transactionId, 0, accountId, amount))
                transactions.commit(transactionId)
                accounts.applyCommands(listOf(AccountsClient.ApplyCommand(accountId, transactionId, amount)))
                payments.resolve(transactionId)
            }
            .flatMap { ServerResponse.ok().build() }
            .handleErrors()

    fun transfer(request: ServerRequest): Mono<ServerResponse> = request
            .bodyToMono(TransferMoney::class.java)
            .map { (fromAccount, toAccount, amount) ->
                val transactionId = transactions.begin().id
                accounts.createDebitCommand(AccountsClient.CreateDebitCommand(fromAccount, transactionId, amount))
                accounts.createCreditCommand(AccountsClient.CreateCreditCommand(toAccount, transactionId))
                payments.create(PaymentsClient.CreatePayment(transactionId, fromAccount, toAccount, amount))
                transactions.commit(transactionId)
                accounts.applyCommands(listOf(
                        AccountsClient.ApplyCommand(fromAccount, transactionId, amount),
                        AccountsClient.ApplyCommand(toAccount, transactionId, amount)
                ))
                payments.resolve(transactionId)
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
