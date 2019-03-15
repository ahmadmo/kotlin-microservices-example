package transaction

import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import javax.persistence.EntityNotFoundException

object FinishedTransactionException : Exception()

@Component
class ExceptionResponseStatusMapper {

    fun map(error: Throwable): ResponseStatusException = when (error) {
        is EntityNotFoundException -> ResponseStatusException(HttpStatus.NOT_FOUND, error.message)
        is FinishedTransactionException -> ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "Transaction Is Not Finished")
        is OptimisticLockingFailureException -> ResponseStatusException(HttpStatus.CONFLICT, "Race Condition")
        else -> ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, error.message, error)
    }
}
