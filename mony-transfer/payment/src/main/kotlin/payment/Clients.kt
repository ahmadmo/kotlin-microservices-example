package payment

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import java.time.OffsetDateTime

@FeignClient(name = "transaction-service", path = "/transactions")
interface TransactionsClient {

    enum class TransactionState {
        IN_PROGRESS,
        ABORTED,
        COMMITTED
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Transaction(val id: Long, val state: TransactionState, val createdAt: OffsetDateTime)

    @RequestMapping(path = ["/{ids}"], method = [RequestMethod.GET], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun findByIds(@PathVariable ids: List<Long>): List<Transaction>
}
