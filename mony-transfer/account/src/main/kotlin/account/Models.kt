package account

import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.Serializable
import java.time.OffsetDateTime
import javax.persistence.*

@Entity
data class Account(
        @Id @GeneratedValue(strategy = GenerationType.AUTO)
        var id: Int = 0,
        @Column(nullable = false)
        var name: String,
        @Column(nullable = false)
        var balance: Long = 0,
        @Column(nullable = false)
        val createdAt: OffsetDateTime = OffsetDateTime.now(),
        var updatedAt: OffsetDateTime? = null,
        @OneToMany(mappedBy = "id.account", cascade = [CascadeType.ALL], orphanRemoval = true)
        var pendingCommands: MutableList<Command> = mutableListOf(),
        @Version
        var version: Int = 0
) : Serializable

enum class CommandType {
    DEBIT,
    CREDIT
}

@Embeddable
data class CommandId(
        @ManyToOne(optional = false)
        @JsonIgnore
        val account: Account,
        @Column(nullable = false)
        val transactionId: Long
) : Serializable

@Entity
data class Command(
        @EmbeddedId
        val id: CommandId,
        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        val type: CommandType,
        @Column(nullable = false)
        val nodeId: Int
) : Serializable {

    override fun toString(): String =
            "Command(transactionId=${id.transactionId}, type=$type, nodeId=$nodeId)"
}
