package account

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface AccountRepository : JpaRepository<Account, Int> {

    @Query("select t.id.account from Command t where t.nodeId = :nodeId")
    fun findAccountsHavingPendingCommandsByNodeId(@Param("nodeId") nodeId: Int): Iterable<Account>
}
