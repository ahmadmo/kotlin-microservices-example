package edge

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

@Component
class EdgeServiceHandler(private val transactions: TransactionsClient,
                         private val accounts: AccountsClient,
                         private val payments: PaymentsClient,
                         private val exceptionMapper: ExceptionResponseStatusMapper) {

    fun debit(request: ServerRequest): Mono<ServerResponse> = request
            .bodyToMono(Debit::class.java)
            .map { (accountId, amount) ->
                val transactionId = transactions.begin().id
                accounts.createDebitCommand(AccountsClient.CreateCommand(accountId, transactionId, amount))
                payments.create(PaymentsClient.CreatePayment(transactionId, accountId, 0, amount))
                transactions.commit(transactionId)
                accounts.applyCommands(transactionId)
                payments.resolve(transactionId)
            }
            .flatMap { ServerResponse.ok().build() }
            .onErrorMap(exceptionMapper::map)

    fun credit(request: ServerRequest): Mono<ServerResponse> = request
            .bodyToMono(Credit::class.java)
            .map { (accountId, amount) ->
                val transactionId = transactions.begin().id
                accounts.createCreditCommand(AccountsClient.CreateCommand(accountId, transactionId, amount))
                payments.create(PaymentsClient.CreatePayment(transactionId, 0, accountId, amount))
                transactions.commit(transactionId)
                accounts.applyCommands(transactionId)
                payments.resolve(transactionId)
            }
            .flatMap { ServerResponse.ok().build() }
            .onErrorMap(exceptionMapper::map)

    fun transfer(request: ServerRequest): Mono<ServerResponse> = request
            .bodyToMono(Transfer::class.java)
            .map { (fromAccount, toAccount, amount) ->
                val transactionId = transactions.begin().id
                accounts.createDebitCommand(AccountsClient.CreateCommand(fromAccount, transactionId, amount))
                accounts.createCreditCommand(AccountsClient.CreateCommand(toAccount, transactionId, amount))
                payments.create(PaymentsClient.CreatePayment(transactionId, fromAccount, toAccount, amount))
                transactions.commit(transactionId)
                accounts.applyCommands(transactionId)
                payments.resolve(transactionId)
            }
            .flatMap { ServerResponse.ok().build() }
            .onErrorMap(exceptionMapper::map)
}
