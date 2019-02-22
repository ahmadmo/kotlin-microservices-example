package transaction

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TransactionRepository : CrudRepository<Transaction, Long> {

    fun findAllByNodeId(nodeId: Int): Iterable<Transaction>
}
