package transaction

import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.router

@Component
class TransactionServiceRouter(private val handler: TransactionServiceHandler) {

    @Bean
    fun transactionsRouter() = router {
        path("/transactions").nest {
            PUT("", handler::begin)
            GET("/{ids:\\d+(?:,\\d+)*}", handler::findAllById)
            POST("/{id:\\d+}/abort", handler::abort)
            POST("/{id:\\d+}/commit", handler::commit)
        }
    }
}
