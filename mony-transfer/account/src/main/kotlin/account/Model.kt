package account

import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.Serializable
import java.time.OffsetDateTime
import javax.persistence.*
import kotlin.math.abs

@Entity
data class Account(
        @Id @GeneratedValue(strategy = GenerationType.AUTO)
        var id: Int = 0,
        @Column(nullable = false)
        var name: String,
        @Column(nullable = false)
        var balance: Long = 0,
        @OneToMany(mappedBy = "id.account", cascade = [CascadeType.ALL], orphanRemoval = true)
        var commands: MutableList<Command> = mutableListOf(),
        @Column(nullable = false)
        val createdAt: OffsetDateTime = OffsetDateTime.now(),
        var updatedAt: OffsetDateTime? = null,
        @Version
        var version: Int = 0
) : Serializable

@Embeddable
data class CommandId(
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JsonIgnore
        val account: Account,
        @Column(nullable = false)
        val transactionId: Long
) : Serializable

@Entity
data class Command(
        @EmbeddedId
        val id: CommandId,
        @Column(nullable = false)
        val amount: Long,
        @Column(nullable = false)
        val nodeId: Int
) : Serializable {

    companion object {

        fun debit(account: Account, transactionId: Long, amount: Long, nodeId: Int) =
                Command(CommandId(account, transactionId), 0 - abs(amount), nodeId)

        fun credit(account: Account, transactionId: Long, amount: Long, nodeId: Int) =
                Command(CommandId(account, transactionId), abs(amount), nodeId)
    }

    init {
        require(amount != 0L) { "Amount cannot be Zero" }
    }

    fun isDebit(): Boolean =
            amount < 0

    override fun toString(): String =
            "Command(transactionId=${id.transactionId}, amount=$amount, nodeId=$nodeId)"
}
