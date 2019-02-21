package edge

import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.router

@Component
class EdgeServiceRouter(private val handler: EdgeServiceHandler) {

    @Bean
    fun accountsRouter() = router {
        (accept(MediaType.APPLICATION_JSON) and "/accounts").nest {
            POST("/debit", handler::debit)
            POST("/credit", handler::credit)
            POST("/transfer", handler::transfer)
        }
    }
}
