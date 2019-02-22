package account

import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.json
import reactor.core.publisher.Mono
import java.net.URI
import javax.persistence.EntityNotFoundException

@Component
class AccountServiceHandler(private val service: AccountService,
                            private val transactions: TransactionsClient) {

    companion object {
        private val commitTransactionsType = object : ParameterizedTypeReference<List<ApplyCommand>>() {}
    }

    fun createAccount(request: ServerRequest): Mono<ServerResponse> = request
            .bodyToMono(CreateAccount::class.java)
            .map { (name) -> service.createAccount(name) }
            .flatMap { id ->
                ServerResponse.created(URI("/accounts/$id")).syncBody(mapOf("id" to id))
            }

    fun findAccountById(request: ServerRequest): Mono<ServerResponse> = Mono
            .fromCallable {
                service.findAccountById(request.pathVariable("id").toInt())
            }
            .flatMap { account ->
                if (account.isPresent) ServerResponse.ok().json().syncBody(account.get())
                else ServerResponse.notFound().build()
            }

    fun findAll(request: ServerRequest): Mono<ServerResponse> {
        val page = request.queryParam("page").map { it.toInt() }.orElse(0)
        val size = request.queryParam("size").map { Math.min(it.toInt(), 50) }.orElse(10)
        return when {
            page < 0 || size < 1 -> ServerResponse.badRequest().build()
            else -> Mono.fromCallable { service.findAll(page, size) }
                    .flatMap { accounts -> ServerResponse.ok().json().syncBody(accounts) }
        }
    }

    fun deleteAccountById(request: ServerRequest): Mono<ServerResponse> = Mono
            .fromCallable {
                service.deleteAccountById(request.pathVariable("id").toInt())
            }
            .flatMap { ServerResponse.ok().build() }

    fun createDebitCommand(request: ServerRequest): Mono<ServerResponse> = request
            .bodyToMono(CreateDebitCommand::class.java)
            .map { (accountId, transactionId, amount) ->
                service.createDebitCommand(accountId, transactionId, amount)
            }
            .flatMap { ServerResponse.ok().build() }
            .handleErrors()

    fun createCreditCommand(request: ServerRequest): Mono<ServerResponse> = request
            .bodyToMono(CreateCreditCommand::class.java)
            .map { (accountId, transactionId) ->
                service.createCreditCommand(accountId, transactionId)
            }
            .flatMap { ServerResponse.ok().build() }
            .handleErrors()

    fun applyCommands(request: ServerRequest): Mono<ServerResponse> = request
            .bodyToMono(commitTransactionsType)
            .map { commands ->
                val transactions = transactions.findByIds(commands.map { it.transactionId })
                if (transactions.any { it.state == TransactionsClient.TransactionState.IN_PROGRESS }) {
                    throw TransactionNotFinishedException
                }
                service.applyCommands(commands, transactions)
            }
            .flatMap { ServerResponse.ok().build() }
            .handleErrors()
}

fun Mono<ServerResponse>.handleErrors(): Mono<ServerResponse> = this
        .onErrorResume(EntityNotFoundException::class.java) {
            ServerResponse.status(HttpStatus.NOT_FOUND).syncBody(it.message ?: "")
        }
        .onErrorResume(BusyAccountException::class.java) {
            ServerResponse.status(HttpStatus.CONFLICT).syncBody("Busy Account")
        }
        .onErrorResume(NotEnoughBalanceException::class.java) {
            ServerResponse.badRequest().syncBody("Not Enough Balance")
        }
        .onErrorResume(TransactionNotFinishedException::class.java) {
            ServerResponse.badRequest().syncBody("Transaction Not Finished")
        }
