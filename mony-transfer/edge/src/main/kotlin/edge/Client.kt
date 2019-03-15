package edge

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@FeignClient(name = "transaction-service", path = "/transactions")
interface TransactionsClient {

    data class TransactionId(val id: Long)

    @PutMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun begin(): TransactionId

    @PostMapping(path = ["/{id}/abort"])
    fun abort(@PathVariable id: Long)

    @PostMapping(path = ["/{id}/commit"])
    fun commit(@PathVariable id: Long)
}

@FeignClient(name = "account-service", path = "/accounts/commands")
interface AccountsClient {

    data class CreateCommand(val accountId: Int, val transactionId: Long, val amount: Long)

    @PutMapping(path = ["/debit"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun createDebitCommand(@RequestBody body: CreateCommand)

    @PutMapping(path = ["/credit"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun createCreditCommand(@RequestBody body: CreateCommand)

    @PostMapping(path = ["/{transactionId}"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun applyCommands(@PathVariable transactionId: Long)
}

@FeignClient(name = "payment-service", path = "/payments")
interface PaymentsClient {

    data class CreatePayment(val transactionId: Long, val fromAccount: Int, val toAccount: Int, val amount: Long)

    @PutMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun create(@RequestBody body: CreatePayment)

    @PostMapping(path = ["/{transactionId}"])
    fun resolve(@PathVariable transactionId: Long)
}
