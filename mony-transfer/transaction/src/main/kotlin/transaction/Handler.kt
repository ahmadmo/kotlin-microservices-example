package transaction

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.json
import reactor.core.publisher.Mono
import java.net.URI

@Component
class TransactionServiceHandler(private val service: TransactionService,
                                private val watcher: TransactionWatcher,
                                private val exceptionMapper: ExceptionResponseStatusMapper) {

    fun begin(request: ServerRequest): Mono<ServerResponse> = Mono
            .fromCallable {
                val transaction = service.begin()
                watcher.watch(transaction)
                transaction.id
            }
            .flatMap { id ->
                ServerResponse.created(URI("/transactions/$id")).syncBody(mapOf("id" to id))
            }

    fun findAllById(request: ServerRequest): Mono<ServerResponse> = Mono
            .fromCallable {
                val ids = request.pathVariable("ids").split(',')
                        .asSequence().take(50).map { it.toLong() }
                        .toList()
                service.findAllById(ids)
            }
            .flatMap { transactions ->
                ServerResponse.ok().json().syncBody(transactions)
            }

    fun abort(request: ServerRequest): Mono<ServerResponse> =
            changeState(request, TransactionState.ABORTED)

    fun commit(request: ServerRequest): Mono<ServerResponse> =
            changeState(request, TransactionState.COMMITTED)

    private fun changeState(request: ServerRequest, state: TransactionState): Mono<ServerResponse> = Mono
            .fromCallable {
                val id = request.pathVariable("id").toLong()
                service.changeState(id, state)
                watcher.unwatch(id)
            }
            .flatMap { ServerResponse.ok().build() }
            .onErrorMap(exceptionMapper::map)
}
