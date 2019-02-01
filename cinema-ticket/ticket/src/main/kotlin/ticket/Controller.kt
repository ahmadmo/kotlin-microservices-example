package ticket

import common.Controller
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

object TicketController : Controller({

    put("/tickets").handler { ctx ->
        val id = TicketService.add()
        ctx.response()
            .setStatusCode(201)
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(JsonObject().put("id", id).encodePrettily())
    }

    get("/tickets").handler { ctx ->
        val tickets = TicketService.list().map {
            JsonObject().put("id", it.id).put("customerId", it.customerId)
        }
        ctx.response()
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(JsonArray().apply { tickets.forEach { add(it) } }.encodePrettily())
    }

    post("/tickets/:tickets/customers/:customerId").handler { ctx ->
        val customerId = ctx.pathParam("customerId").toInt()
        val tickets = ctx.pathParam("tickets").splitToSequence(",").map { it.toInt() }.toList()
        val bought = TicketService.buy(tickets, customerId)
        ctx.response().apply {
            statusCode = if (bought) 200 else 409
            end()
        }
    }

    get("/tickets/customers/:customerId").handler { ctx ->
        val customerId = ctx.pathParam("customerId").toInt()
        val tickets = TicketService.list(customerId).map {
            JsonObject().put("id", it.id).put("customerId", it.customerId)
        }
        ctx.response()
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(JsonArray().apply { tickets.forEach { add(it) } }.encodePrettily())
    }
})
