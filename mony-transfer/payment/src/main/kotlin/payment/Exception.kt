package payment

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.netflix.client.ClientException
import com.netflix.hystrix.exception.HystrixRuntimeException
import feign.FeignException
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.nio.charset.StandardCharsets
import javax.persistence.EntityNotFoundException

object NotFinishedTransactionException : Exception()

@Component
class ExceptionResponseStatusMapper(private val json: ObjectMapper) {

    fun map(error: Throwable): ResponseStatusException = when (error) {
        is HystrixRuntimeException -> {
            val cause = error.cause?.cause ?: error.cause
            when (cause) {
                is FeignException -> {
                    val status = HttpStatus.resolve(cause.status()) ?: HttpStatus.SERVICE_UNAVAILABLE
                    val message = cause.content()?.toString(StandardCharsets.UTF_8)?.let {
                        json.readValue(it, ObjectNode::class.java).get("message").textValue()
                    }
                    ResponseStatusException(status, message)
                }
                is ClientException -> ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, cause.errorMessage)
                else -> ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, cause?.message, cause)
            }
        }
        is EntityNotFoundException -> ResponseStatusException(HttpStatus.NOT_FOUND, error.message)
        is NotFinishedTransactionException -> ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "Transaction Is Not Finished")
        is DuplicateKeyException -> ResponseStatusException(HttpStatus.CONFLICT, "Race Condition")
        else -> ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, error.message, error)
    }
}
