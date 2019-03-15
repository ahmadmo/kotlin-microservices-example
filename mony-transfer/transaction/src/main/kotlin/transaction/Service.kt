package transaction

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import javax.persistence.EntityNotFoundException

@Service
class TransactionService(private val repo: TransactionRepository,
                         private val nodeProps: NodeProperties) {

    fun begin(): Transaction =
            repo.save(Transaction(nodeId = nodeProps.id))

    fun findAllById(ids: List<Long>): Iterable<Transaction> =
            repo.findAllById(ids)

    fun findAllInProgress(): Iterable<Transaction> =
            repo.findAllByStateAndNodeId(TransactionState.IN_PROGRESS, nodeProps.id)

    @Transactional
    @Throws(EntityNotFoundException::class, FinishedTransactionException::class)
    fun changeState(id: Long, state: TransactionState) {
        val transaction = repo.findByIdOrNull(id) ?: throw EntityNotFoundException()
        if (transaction.state != TransactionState.IN_PROGRESS) {
            throw FinishedTransactionException
        }
        transaction.state = state
        transaction.updatedAt = OffsetDateTime.now()
    }
}
