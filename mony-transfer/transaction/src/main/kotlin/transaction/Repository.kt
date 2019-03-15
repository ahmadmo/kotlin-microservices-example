package transaction

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TransactionRepository : CrudRepository<Transaction, Long> {

    fun findAllByStateAndNodeId(state: TransactionState, nodeId: Int): Iterable<Transaction>
}
