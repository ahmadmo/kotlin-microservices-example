package transaction

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.json
import reactor.core.publisher.Mono
import java.net.URI

@Component
class TransactionServiceHandler(private val service: TransactionService) {

    fun begin(request: ServerRequest): Mono<ServerResponse> =
            Mono.fromCallable { service.begin() }
                    .flatMap { id -> ServerResponse.created(URI("/transactions/$id")).syncBody(mapOf("id" to id)) }

    fun findByIds(request: ServerRequest): Mono<ServerResponse> =
            Mono.fromCallable { service.findByIds(request.pathVariable("ids").split(',').map { it.toLong() }) }
                    .flatMap { transactions -> ServerResponse.ok().json().syncBody(transactions) }

    fun abort(request: ServerRequest): Mono<ServerResponse> =
            changeState(request, TransactionState.ABORTED)

    fun commit(request: ServerRequest): Mono<ServerResponse> =
            changeState(request, TransactionState.COMMITTED)

    private fun changeState(request: ServerRequest, state: TransactionState): Mono<ServerResponse> =
            Mono.fromCallable { service.changeState(request.pathVariable("id").toLong(), state) }
                    .flatMap { ServerResponse.ok().build() }
                    .onErrorResume(IllegalStateException::class.java) {
                        ServerResponse.badRequest().syncBody("Transaction must be in progress")
                    }
}
