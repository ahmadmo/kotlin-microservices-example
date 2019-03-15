package account

import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.router

@Component
class AccountServiceRouter(private val handler: AccountServiceHandler) {

    @Bean
    fun accountsRouter() = router {
        path("/accounts").nest {
            accept(MediaType.APPLICATION_JSON).nest {
                PUT("", handler::createAccount)
            }
            GET("/{id:\\d+}", handler::findAccountById)
            GET("", handler::findAll)
            DELETE("/{id:\\d+}", handler::deleteAccountById)
            (accept(MediaType.APPLICATION_JSON) and "/commands").nest {
                PUT("/debit", handler::createDebitCommand)
                PUT("/credit", handler::createCreditCommand)
                POST("/{id:\\d+}", handler::applyCommands)
            }
        }
    }
}
