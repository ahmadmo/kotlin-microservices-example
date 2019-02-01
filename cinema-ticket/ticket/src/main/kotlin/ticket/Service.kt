package ticket

import ticket.TicketDatabase.withTransaction

object TicketService {

    fun add(): Int = withTransaction {
        Tickets.insert()
    }

    fun list(): List<Ticket> = withTransaction {
        Tickets.select()
    }

    fun buy(tickets: List<Int>, customerId: Int): Boolean = withTransaction {
        Tickets.update(tickets, customerId)
    }

    fun list(customerId: Int): List<Ticket> = withTransaction {
        Tickets.select(customerId)
    }
}
