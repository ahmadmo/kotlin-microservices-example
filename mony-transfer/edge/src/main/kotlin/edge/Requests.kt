package edge

data class Debit(val accountId: Int, val amount: Long)
data class Credit(val accountId: Int, val amount: Long)
data class TransferMoney(val fromAccount: Int, val toAccount: Int, val amount: Long)
