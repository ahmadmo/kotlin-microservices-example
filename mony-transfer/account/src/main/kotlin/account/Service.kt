package account

import org.hibernate.Hibernate
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.*
import javax.persistence.EntityNotFoundException

@Service
class AccountService(private val repo: AccountRepository,
                     private val nodeProps: NodeProperties) {

    fun createAccount(name: String): Int =
            repo.save(Account(name = name)).id

    @Transactional(readOnly = true)
    fun findAccountById(id: Int): Optional<Account> =
            repo.findById(id).map { Hibernate.initialize(it.commands); it }

    @Transactional(readOnly = true)
    fun findAll(page: Int, size: Int): Page<Account> =
            repo.findAll(PageRequest.of(page, size, Sort.Direction.ASC, "createdAt")).apply {
                forEach { Hibernate.initialize(it.commands) }
            }

    fun deleteAccountById(id: Int) {
        repo.deleteById(id)
    }

    @Transactional
    @Throws(EntityNotFoundException::class, BusyAccountException::class, NotEnoughBalanceException::class)
    fun createDebitCommand(accountId: Int, transactionId: Long, amount: Long) {
        with(repo.findByIdOrNull(accountId) ?: throw EntityNotFoundException()) {
            if (commands.isNotEmpty()) throw BusyAccountException
            if (balance < amount) throw NotEnoughBalanceException
            commands.add(Command.debit(this, transactionId, amount, nodeProps.id))
            updatedAt = OffsetDateTime.now()
        }
    }

    @Transactional
    @Throws(EntityNotFoundException::class, BusyAccountException::class)
    fun createCreditCommand(accountId: Int, transactionId: Long, amount: Long) {
        with(repo.findByIdOrNull(accountId) ?: throw EntityNotFoundException()) {
            if (commands.any { it.isDebit() }) throw BusyAccountException
            commands.add(Command.credit(this, transactionId, amount, nodeProps.id))
            updatedAt = OffsetDateTime.now()
        }
    }
}

@Service
class CommandService(private val repo: CommandRepository,
                     private val nodeProps: NodeProperties) {

    fun findAll(): Iterable<Command> =
            repo.findAllByNodeId(nodeProps.id)

    @Transactional
    @Throws(NotFinishedTransactionException::class, EntityNotFoundException::class)
    fun apply(transaction: TransactionsClient.Transaction) {
        if (transaction.state == TransactionsClient.TransactionState.IN_PROGRESS) {
            throw NotFinishedTransactionException
        }
        val commands = repo.findAllByTransactionId(transaction.id)
        if (commands.isEmpty()) throw EntityNotFoundException()
        for (cmd in commands) {
            val account = cmd.id.account
            account.apply(cmd, transaction.state)
        }
    }

    private fun Account.apply(cmd: Command, state: TransactionsClient.TransactionState) {
        if (state == TransactionsClient.TransactionState.COMMITTED) {
            balance += cmd.amount
        }
        commands.removeIf { it.id.transactionId == cmd.id.transactionId }
        updatedAt = OffsetDateTime.now()
    }
}
