package account

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.json
import reactor.core.publisher.Mono
import java.net.URI
import javax.persistence.EntityNotFoundException

@Component
class AccountServiceHandler(private val accountService: AccountService,
                            private val commandService: CommandService,
                            private val commandWatcher: CommandWatcher,
                            private val transactionsClient: TransactionsClient,
                            private val exceptionMapper: ExceptionResponseStatusMapper) {

    fun createAccount(request: ServerRequest): Mono<ServerResponse> = request
            .bodyToMono(CreateAccount::class.java)
            .map { (name) -> accountService.createAccount(name) }
            .flatMap { id ->
                ServerResponse.created(URI("/accounts/$id")).syncBody(mapOf("id" to id))
            }

    fun findAccountById(request: ServerRequest): Mono<ServerResponse> = Mono
            .fromCallable {
                accountService.findAccountById(request.pathVariable("id").toInt())
            }
            .flatMap { account ->
                if (account.isPresent) ServerResponse.ok().json().syncBody(account.get())
                else ServerResponse.notFound().build()
            }

    fun findAll(request: ServerRequest): Mono<ServerResponse> = Mono
            .fromCallable {
                val page = request.queryParam("page").map { Math.max(it.toInt(), 0) }.orElse(0)
                val size = request.queryParam("size").map { Math.max(Math.min(it.toInt(), 50), 0) }.orElse(10)
                accountService.findAll(page, size)
            }
            .flatMap { accounts ->
                ServerResponse.ok().json().syncBody(accounts.response())
            }

    fun deleteAccountById(request: ServerRequest): Mono<ServerResponse> = Mono
            .fromCallable {
                accountService.deleteAccountById(request.pathVariable("id").toInt())
            }
            .flatMap { ServerResponse.ok().build() }

    fun createDebitCommand(request: ServerRequest): Mono<ServerResponse> = request
            .bodyToMono(CreateCommand::class.java)
            .map { (accountId, transactionId, amount) ->
                accountService.createDebitCommand(accountId, transactionId, amount)
                commandWatcher.watch(transactionId)
            }
            .flatMap { ServerResponse.ok().build() }
            .onErrorMap(exceptionMapper::map)

    fun createCreditCommand(request: ServerRequest): Mono<ServerResponse> = request
            .bodyToMono(CreateCommand::class.java)
            .map { (accountId, transactionId, amount) ->
                accountService.createCreditCommand(accountId, transactionId, amount)
                commandWatcher.watch(transactionId)
            }
            .flatMap { ServerResponse.ok().build() }
            .onErrorMap(exceptionMapper::map)

    fun applyCommands(request: ServerRequest): Mono<ServerResponse> = Mono
            .fromCallable {
                val transactionId = request.pathVariable("id").toLong()
                val transactions = transactionsClient.findAllById(listOf(transactionId))
                val transaction = transactions.firstOrNull() ?: throw EntityNotFoundException()
                if (transaction.state == TransactionsClient.TransactionState.IN_PROGRESS) {
                    throw NotFinishedTransactionException
                }
                commandService.apply(transaction)
                commandWatcher.unwatch(transactionId)
            }
            .flatMap { ServerResponse.ok().build() }
            .onErrorMap(exceptionMapper::map)
}
