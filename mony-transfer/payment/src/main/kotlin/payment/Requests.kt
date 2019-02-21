package payment

data class CreatePayment(val transactionId: Long, val fromAccount: Int, val toAccount: Int, val amount: Long)
