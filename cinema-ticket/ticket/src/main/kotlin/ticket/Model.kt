package ticket

import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.*

object TicketModel {

    fun tables(): Array<Table> = arrayOf(
        Tickets
    )
}

data class Ticket(val id: Int, val customerId: Int?)

object Tickets : IntIdTable() {

    private val customerId = integer("customerId").nullable()

    private fun ResultRow.data() =
        Ticket(this[id].value, this[customerId])

    fun insert(): Int =
        insertAndGetId { it[customerId] = null }
            .value

    fun select(): List<Ticket> =
        selectAll()
            .map { it.data() }

    fun update(ids: List<Int>, customerId: Int): Boolean =
        update(
            where = { Tickets.id inList ids and (Tickets.customerId.isNull() or (Tickets.customerId eq customerId)) },
            body = { it[Tickets.customerId] = customerId }
        ) == ids.size

    fun select(customerId: Int): List<Ticket> =
        select { Tickets.customerId eq customerId }
            .map { it.data() }
}
