package account

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface AccountRepository : JpaRepository<Account, Int>

@Repository
interface CommandRepository : CrudRepository<Command, CommandId> {

    fun findAllByNodeId(nodeId: Int): Iterable<Command>

    @Query("select c from Command c where c.id.transactionId = :transactionId")
    fun findAllByTransactionId(transactionId: Long): List<Command>
}
