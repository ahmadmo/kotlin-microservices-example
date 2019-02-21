package payment

import java.time.OffsetDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class PendingPayment(
        @Id
        val transactionId: Long,
        @Column(nullable = false)
        val fromAccount: Int,
        @Column(nullable = false)
        val toAccount: Int,
        @Column(nullable = false)
        val amount: Long,
        @Column(nullable = false)
        val nodeId: Int
) {

    fun toPayment(createdAt: OffsetDateTime) =
            Payment(transactionId, fromAccount, toAccount, amount, createdAt)
}

@Entity
data class Payment(
        @Id
        val transactionId: Long,
        @Column(nullable = false)
        val fromAccount: Int,
        @Column(nullable = false)
        val toAccount: Int,
        @Column(nullable = false)
        val amount: Long,
        @Column(nullable = false)
        val createdAt: OffsetDateTime
)
