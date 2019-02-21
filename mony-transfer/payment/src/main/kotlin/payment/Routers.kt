package payment

import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.router

@Component
class PaymentServiceRouter(private val handler: PaymentServiceHandler) {

    @Bean
    fun paymentsRouter() = router {
        path("/payments").nest {
            accept(MediaType.APPLICATION_JSON).nest {
                PUT("", handler::create)
            }
            POST("/{id:\\d+}", handler::resolve)
            GET("", handler::findAll)
        }
    }
}
