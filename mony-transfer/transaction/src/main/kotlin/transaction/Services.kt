package transaction

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import javax.persistence.EntityNotFoundException

@Service
class TransactionService(private val repo: TransactionRepository,
                         private val nodeProps: NodeProperties) {

    fun begin(): Long =
            repo.save(Transaction(nodeId = nodeProps.id)).id

    fun findByIds(ids: List<Long>): Iterable<Transaction> =
            repo.findAllById(ids)

    @Transactional
    @Throws(EntityNotFoundException::class, IllegalStateException::class)
    fun changeState(id: Long, state: TransactionState) {
        val transaction = repo.findById(id).orElseThrow { EntityNotFoundException() }
        check(transaction.state == TransactionState.IN_PROGRESS)
        transaction.state = state
        transaction.updatedAt = OffsetDateTime.now()
    }
}
