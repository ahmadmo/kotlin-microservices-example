package payment

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityNotFoundException

@Service
class PaymentService(private val pendingPaymentRepo: PendingPaymentRepository,
                     private val paymentRepo: PaymentRepository,
                     private val nodeProps: NodeProperties) {

    fun create(transactionId: Long, fromAccount: Int, toAccount: Int, amount: Long) =
            pendingPaymentRepo.save(PendingPayment(transactionId, fromAccount, toAccount, amount, nodeProps.id))

    fun findAllPending(): Iterable<PendingPayment> =
            pendingPaymentRepo.findAllByNodeId(nodeProps.id)

    @Transactional
    @Throws(NotFinishedTransactionException::class, EntityNotFoundException::class)
    fun resolve(transaction: TransactionsClient.Transaction) {
        if (transaction.state == TransactionsClient.TransactionState.IN_PROGRESS) {
            throw NotFinishedTransactionException
        }
        val pendingPayment = pendingPaymentRepo.findByIdOrNull(transaction.id) ?: throw EntityNotFoundException()
        if (transaction.state == TransactionsClient.TransactionState.COMMITTED) {
            paymentRepo.save(pendingPayment.toPayment(transaction.createdAt))
        }
        pendingPaymentRepo.delete(pendingPayment)
    }

    fun findAll(page: Int, size: Int): Page<Payment> =
            paymentRepo.findAll(PageRequest.of(page, size, Sort.Direction.DESC, "createdAt"))
}
