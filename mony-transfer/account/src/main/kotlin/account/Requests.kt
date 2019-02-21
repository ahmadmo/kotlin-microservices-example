package account

data class CreateAccount(val name: String)
data class CreateDebitCommand(val accountId: Int, val transactionId: Long, val amount: Long)
data class CreateCreditCommand(val accountId: Int, val transactionId: Long)
data class ApplyCommand(val accountId: Int, val transactionId: Long, val amount: Long)
