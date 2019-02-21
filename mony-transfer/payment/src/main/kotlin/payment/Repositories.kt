package payment

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PendingPaymentRepository : CrudRepository<PendingPayment, Long>

@Repository
interface PaymentRepository : JpaRepository<Payment, Long>
