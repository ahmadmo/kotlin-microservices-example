@file:Suppress("unused")

package customer

import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.*

object CustomerModel {

    fun tables(): Array<Table> = arrayOf(
        Customers, Jobs
    )
}

data class Customer(val id: Int, val name: String)

object Customers : IntIdTable() {

    private val name = varchar("name", 100)

    private fun ResultRow.data() =
        Customer(this[id].value, this[name])

    fun insert(name: String): Int =
        insertAndGetId { it[Customers.name] = name }
            .value

    fun select(id: Int): Customer? =
        select { Customers.id eq id }
            .map { it.data() }
            .firstOrNull()

    fun select(): List<Customer> =
        selectAll()
            .map { it.data() }
}

enum class JobStatus { IN_PROGRESS, DONE, CANCELLED }
data class Job(val id: Int, val status: JobStatus, val data: String)

object Jobs : IntIdTable() {

    private val status = enumeration("status", JobStatus::class)
    private val data = text("data")

    private fun ResultRow.data() =
        Job(this[id].value, this[status], this[data])

    fun insert(data: String): Int =
        insertAndGetId {
            it[Jobs.status] = JobStatus.IN_PROGRESS
            it[Jobs.data] = data
        }.value

    fun select(id: Int): Job? =
        select { Jobs.id eq id }
            .map { it.data() }
            .firstOrNull()

    fun update(id: Int, status: JobStatus): Boolean =
        update(
            where = { Jobs.id eq id },
            body = { it[Jobs.status] = status }
        ) == 1

    fun select(status: JobStatus): List<Job> =
        select { Jobs.status eq status }
            .map { it.data() }

    fun delete(id: Int): Boolean =
        deleteWhere { Jobs.id eq id } == 1
}
