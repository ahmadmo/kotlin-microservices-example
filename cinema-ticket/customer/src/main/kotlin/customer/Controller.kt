package customer

import common.Controller
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

object CustomerController : Controller({

    put("/customers/:name").handler { ctx ->
        val name = ctx.pathParam("name")
        val id = CustomerService.add(name)
        ctx.response()
            .setStatusCode(201)
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(JsonObject().put("id", id).encodePrettily())
    }

    get("/customers/:id").handler { ctx ->
        val id = ctx.pathParam("id").toInt()
        val customer = CustomerService.get(id)
        if (customer == null) {
            ctx.response().apply {
                statusCode = 404
                end()
            }
        } else {
            ctx.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(JsonObject().put("id", customer.id).put("name", customer.name).encodePrettily())
        }
    }

    get("/customers").handler { ctx ->
        val customers = CustomerService.list().map {
            JsonObject().put("id", it.id).put("name", it.name)
        }
        ctx.response()
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(JsonArray().apply { customers.forEach { add(it) } }.encodePrettily())
    }
})

object JobController : Controller({

    get("/jobs/:id").handler { ctx ->
        val id = ctx.pathParam("id").toInt()
        val status = JobService.getStatus(id)
        if (status == null) {
            ctx.response().apply {
                statusCode = 404
                end()
            }
        } else {
            ctx.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(JsonObject().put("id", id).put("status", status).encodePrettily())
        }
    }
})

object CustomerTicketsController : Controller({

    post("/customers/:customerId/tickets/:tickets").handler { ctx ->
        val customerId = ctx.pathParam("customerId").toInt()
        val tickets = ctx.pathParam("tickets").splitToSequence(",").map { it.toInt() }.toList()
        val jobId = CustomerTicketsService.buy(customerId, tickets)
        ctx.response()
            .setStatusCode(202)
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(JsonObject().put("jobId", jobId).encodePrettily())
    }

    get("/customers/:customerId/tickets").handler { ctx ->
        val customerId = ctx.pathParam("customerId").toInt()
        CustomerTicketsService.list(customerId).whenComplete { tickets, _ ->
            if (tickets == null) {
                ctx.response().apply {
                    statusCode = 503
                    end()
                }
            } else {
                ctx.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(JsonArray().apply { tickets.forEach { add(it) } }.encodePrettily())
            }
        }
    }
})
