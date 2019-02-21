package transaction

import java.time.OffsetDateTime
import javax.persistence.*

enum class TransactionState {
    IN_PROGRESS,
    ABORTED,
    COMMITTED
}

@Entity
data class Transaction(
        @Id @GeneratedValue(strategy = GenerationType.AUTO)
        var id: Long = 0,
        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        var state: TransactionState = TransactionState.IN_PROGRESS,
        @Column(nullable = false)
        val createdAt: OffsetDateTime = OffsetDateTime.now(),
        var updatedAt: OffsetDateTime? = null,
        @Version
        var version: Int = 0
)
