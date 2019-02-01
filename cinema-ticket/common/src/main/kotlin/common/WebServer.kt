package common

import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router
import java.util.concurrent.CompletableFuture
import kotlin.concurrent.thread

class WebServer(config: HttpServerConfig, controller: Controller) {

    private val vertx = Vertx.vertx()
    private val router = Router.router(vertx)
    private val server = vertx.createHttpServer(HttpServerOptions().apply {
        host = config.bind.host
        port = config.bind.port
    })

    init {
        Runtime.getRuntime().addShutdownHook(thread(start = false) {
            server.close()
            vertx.close()
        })

        controller.mapping.invoke(router)
    }

    fun start() {
        val promise = CompletableFuture<Unit>()
        server.requestHandler(router).listen { ar ->
            if (ar.succeeded()) promise.complete(Unit)
            else promise.completeExceptionally(ar.cause())
        }
        promise.join()
    }
}
