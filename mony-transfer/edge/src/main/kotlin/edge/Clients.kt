package edge

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@FeignClient(name = "transaction-service", path = "/transactions")
interface TransactionsClient {

    data class TransactionId(val id: Long)

    @RequestMapping(method = [RequestMethod.PUT], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun begin(): TransactionId

    @RequestMapping(path = ["/{id}/abort"], method = [RequestMethod.POST])
    fun abort(@PathVariable id: Long)

    @RequestMapping(path = ["/{id}/commit"], method = [RequestMethod.POST])
    fun commit(@PathVariable id: Long)
}

@FeignClient(name = "account-service", path = "/accounts/commands")
interface AccountsClient {

    data class CreateDebitCommand(val accountId: Int, val transactionId: Long, val amount: Long)
    data class CreateCreditCommand(val accountId: Int, val transactionId: Long)
    data class ApplyCommand(val accountId: Int, val transactionId: Long, val amount: Long)

    @RequestMapping(path = ["/debit"], method = [RequestMethod.PUT], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun createDebitCommand(@RequestBody body: CreateDebitCommand)

    @RequestMapping(path = ["/credit"], method = [RequestMethod.PUT], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun createCreditCommand(@RequestBody body: CreateCreditCommand)

    @RequestMapping(method = [RequestMethod.POST], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun applyCommands(@RequestBody body: List<ApplyCommand>)
}

@FeignClient(name = "payment-service", path = "/payments")
interface PaymentsClient {

    data class CreatePayment(val transactionId: Long, val fromAccount: Int, val toAccount: Int, val amount: Long)

    @RequestMapping(method = [RequestMethod.PUT], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun create(@RequestBody body: CreatePayment)

    @RequestMapping(path = ["/{id}"], method = [RequestMethod.POST])
    fun resolve(@PathVariable id: Long)
}
