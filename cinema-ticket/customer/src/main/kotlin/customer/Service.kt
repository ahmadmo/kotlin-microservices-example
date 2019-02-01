package customer

import customer.CustomerDatabase.withTransaction
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import mu.KotlinLogging
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

object CustomerService {

    fun add(name: String): Int = withTransaction {
        Customers.insert(name)
    }

    fun get(id: Int): Customer? = withTransaction {
        Customers.select(id)
    }

    fun list(): List<Customer> = withTransaction {
        Customers.select()
    }
}

object JobService {

    fun add(data: String): Int = withTransaction {
        Jobs.insert(data)
    }

    fun getStatus(id: Int): JobStatus? = withTransaction {
        Jobs.select(id)?.status
    }

    fun setStatus(id: Int, status: JobStatus): Boolean = withTransaction {
        Jobs.update(id, status)
    }

    fun listInProgress(): List<Job> = withTransaction {
        Jobs.select(JobStatus.IN_PROGRESS)
    }

    fun delete(id: Int): Boolean = withTransaction {
        Jobs.delete(id)
    }
}

object CustomerTicketsService {

    private val logger = KotlinLogging.logger {}

    private val vertx = Vertx.vertx()
    private val client = WebClient.create(vertx)

    private val ticketService = CustomerConfig.serviceLocator.ticket

    init {
        Runtime.getRuntime().addShutdownHook(thread(start = false) {
            client.close()
            vertx.close()
        })
    }

    private fun callRemoteBuy(jobId: Int, customerId: Int, tickets: List<Int>, handler: Handler<Int>) {
        val path = "/tickets/${tickets.joinToString(",")}/customers/$customerId"
        logger.debug { "remote call [method = \"POST\", path = $path]" }
        client.post(ticketService.port, ticketService.host, path).send { ar ->
            if (ar.succeeded()) {
                handler.handle(ar.result().statusCode())
            } else {
                logger.error(ar.cause()) { "remote error [method = \"POST\", path = $path]" }
                // FIXME use circuit breaker
                callRemoteBuy(jobId, customerId, tickets, handler)
            }
        }
    }

    fun buy(customerId: Int, tickets: List<Int>): Int {
        val jobData = JsonObject().apply {
            put("customerId", customerId)
            put("tickets", JsonArray().apply { tickets.forEach { add(it) } })
        }
        val jobId = JobService.add(jobData.encode())
        callRemoteBuy(jobId, customerId, tickets, Handler { statusCode ->
            // FIXME handle DB error
            val status = when (statusCode) {
                200 -> JobStatus.DONE
                409 -> JobStatus.CANCELLED
                else -> JobStatus.IN_PROGRESS
            }
            JobService.setStatus(jobId, status)
            logger.debug { "job status changed [jobId = $jobId, status = $status]" }
        })
        return jobId
    }

    fun list(customerId: Int): CompletionStage<List<Int>?> {
        val promise = CompletableFuture<List<Int>>()
        val path = "/tickets/customers/$customerId"
        client.get(ticketService.port, ticketService.host, path).send { ar ->
            if (ar.succeeded()) {
                val tickets = ar.result().bodyAsJsonArray()
                promise.complete(tickets.map { (it as JsonObject).getInteger("id") })
            } else {
                logger.error(ar.cause()) { "remote error [method = \"GET\", path = $path]" }
                promise.complete(null)
            }
        }
        return promise
    }

    fun recover() {
        JobService.listInProgress().forEach { it.recover() }
    }

    private fun Job.recover() {
        val json = JsonObject(data)
        val customerId = json.getInteger("customerId")
        val tickets = json.getJsonArray("tickets").map { it as Int }
        val latch = CountDownLatch(1)
        callRemoteBuy(id, customerId, tickets, Handler {
            JobService.delete(id)
            logger.info { "job recovered [jobId = $id]" }
            latch.countDown()
        })
        latch.await()
    }
}
