package account

import org.hibernate.Hibernate
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.*
import javax.persistence.EntityNotFoundException
import kotlin.collections.HashMap

@Service
class AccountService(private val accountRepo: AccountRepository,
                     private val nodeProps: NodeProperties) {

    fun createAccount(name: String): Int =
            accountRepo.save(Account(name = name)).id

    @Transactional(readOnly = true)
    fun findAccountById(id: Int): Optional<Account> =
            accountRepo.findById(id).map { Hibernate.initialize(it.pendingCommands); it }

    @Transactional(readOnly = true)
    fun findAll(page: Int, size: Int): Iterable<Account> =
            accountRepo.findAll(PageRequest.of(page, size)).content.apply {
                forEach { Hibernate.initialize(it.pendingCommands) }
            }

    fun deleteAccountById(id: Int) {
        accountRepo.deleteById(id)
    }

    @Transactional
    @Throws(EntityNotFoundException::class, BusyAccountException::class, NotEnoughBalanceException::class)
    fun createDebitCommand(accountId: Int, transactionId: Long, amount: Long) {
        val account = accountRepo.findByIdOrNull(accountId) ?: throw EntityNotFoundException()
        when {
            account.pendingCommands.isNotEmpty() -> throw BusyAccountException
            account.balance < amount -> throw NotEnoughBalanceException
            else -> {
                val cmd = Command(CommandId(account, transactionId), CommandType.DEBIT, nodeProps.id)
                account.pendingCommands.add(cmd)
                account.updatedAt = OffsetDateTime.now()
            }
        }
    }

    @Transactional
    @Throws(EntityNotFoundException::class, BusyAccountException::class)
    fun createCreditCommand(accountId: Int, transactionId: Long) {
        val account = accountRepo.findByIdOrNull(accountId) ?: throw EntityNotFoundException()
        if (account.pendingCommands.any { it.type == CommandType.DEBIT }) {
            throw BusyAccountException
        } else {
            val cmd = Command(CommandId(account, transactionId), CommandType.CREDIT, nodeProps.id)
            account.pendingCommands.add(cmd)
            account.updatedAt = OffsetDateTime.now()
        }
    }

    @Transactional
    @Throws(EntityNotFoundException::class)
    fun applyCommands(commands: List<ApplyCommand>, transactions: List<TransactionsClient.Transaction>) {
        val transactionMap = transactions.asSequence().map { it.id to it }.toMap(HashMap())
        for ((accountId, commandGroup) in commands.groupBy { it.accountId }) {
            val account = accountRepo.findByIdOrNull(accountId) ?: throw EntityNotFoundException()
            for (cmd in commandGroup.distinctBy { it.transactionId }) {
                val transaction = transactionMap[cmd.transactionId] ?: throw EntityNotFoundException()
                if (transaction.state != TransactionsClient.TransactionState.IN_PROGRESS) {
                    account.applyCommand(transaction, cmd.amount)
                }
            }
            account.updatedAt = OffsetDateTime.now()
        }
    }

    private fun Account.applyCommand(transaction: TransactionsClient.Transaction, amount: Long) {
        val index = pendingCommands.indexOfFirst { it.id.transactionId == transaction.id }
        if (index == -1) {
            throw EntityNotFoundException()
        }
        val cmd = pendingCommands.removeAt(index)
        if (transaction.state == TransactionsClient.TransactionState.COMMITTED) {
            if (cmd.type == CommandType.CREDIT) {
                balance += amount
            } else {
                balance -= amount
            }
        }
    }
}
