package payment

import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import javax.persistence.EntityNotFoundException

@Service
class PaymentService(private val pendingPaymentRepo: PendingPaymentRepository,
                     private val paymentRepo: PaymentRepository,
                     private val nodeProps: NodeProperties) {

    fun create(transactionId: Long, fromAccount: Int, toAccount: Int, amount: Long) {
        pendingPaymentRepo.save(PendingPayment(transactionId, fromAccount, toAccount, amount, nodeProps.id))
    }

    @Transactional
    @Throws(EntityNotFoundException::class)
    fun abort(transactionId: Long) {
        val pendingPayment = pendingPaymentRepo.findByIdOrNull(transactionId) ?: throw EntityNotFoundException()
        pendingPaymentRepo.delete(pendingPayment)
    }

    @Transactional
    @Throws(EntityNotFoundException::class)
    fun commit(transactionId: Long, createdAt: OffsetDateTime) {
        val pendingPayment = pendingPaymentRepo.findByIdOrNull(transactionId) ?: throw EntityNotFoundException()
        paymentRepo.save(pendingPayment.toPayment(createdAt))
        pendingPaymentRepo.delete(pendingPayment)
    }

    fun findAll(page: Int, size: Int): Iterable<Payment> =
            paymentRepo.findAll(PageRequest.of(page, size)).content
}
