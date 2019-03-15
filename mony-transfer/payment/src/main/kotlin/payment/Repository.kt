package payment

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PendingPaymentRepository : CrudRepository<PendingPayment, Long> {

    fun findAllByNodeId(nodeId: Int): Iterable<PendingPayment>
}

@Repository
interface PaymentRepository : JpaRepository<Payment, Long>
