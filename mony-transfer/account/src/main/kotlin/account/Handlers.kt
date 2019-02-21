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
                            private val transactionsClient: TransactionsClient) {

    companion object {
        private val commitTransactionsType = object : ParameterizedTypeReference<List<ApplyCommand>>() {}
    }

    fun createAccount(request: ServerRequest): Mono<ServerResponse> =
            request.bodyToMono(CreateAccount::class.java)
                    .map { (name) -> service.createAccount(name) }
                    .flatMap { id -> ServerResponse.created(URI("/accounts/$id")).syncBody(mapOf("id" to id)) }

    fun findAccountById(request: ServerRequest): Mono<ServerResponse> =
            Mono.fromCallable { service.findAccountById(request.pathVariable("id").toInt()) }
                    .flatMap { account ->
                        if (account.isPresent) ServerResponse.ok().json().syncBody(account.get())
                        else ServerResponse.notFound().build()
                    }

    fun deleteAccountById(request: ServerRequest): Mono<ServerResponse> =
            Mono.fromCallable { service.deleteAccountById(request.pathVariable("id").toInt()) }
                    .flatMap { ServerResponse.ok().build() }

    fun createDebitCommand(request: ServerRequest): Mono<ServerResponse> =
            request.bodyToMono(CreateDebitCommand::class.java)
                    .map { (accountId, transactionId, amount) ->
                        service.createDebitCommand(accountId, transactionId, amount)
                    }
                    .flatMap { ServerResponse.ok().build() }
                    .handleErrors()

    fun createCreditCommand(request: ServerRequest): Mono<ServerResponse> =
            request.bodyToMono(CreateCreditCommand::class.java)
                    .map { (accountId, transactionId) ->
                        service.createCreditCommand(accountId, transactionId)
                    }
                    .flatMap { ServerResponse.ok().build() }
                    .handleErrors()

    fun applyCommands(request: ServerRequest): Mono<ServerResponse> =
            request.bodyToMono(commitTransactionsType)
                    .map { commands ->
                        val transactions = transactionsClient.findByIds(commands.map { it.transactionId })
                        commands to transactions
                    }
                    .map { (commands, transactions) ->
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
