package common

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SchemaUtils.withDataBaseLock
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.concurrent.thread
import org.jetbrains.exposed.sql.Database as ExposedDatabase

open class Database(config: DatabaseConfig) {

    private val ds: HikariDataSource = HikariDataSource(HikariConfig().apply {
        jdbcUrl = config.url
        username = config.user
        password = config.pass
    })

    private val db = ExposedDatabase.connect(ds)

    init {
        Runtime.getRuntime().addShutdownHook(thread(start = false) {
            ds.close()
        })

        Configurator.setLevel("Exposed", Level.valueOf(config.logLevel))
    }

    fun <T> withTransaction(statement: Transaction.() -> T): T =
        transaction(db, statement)

    fun createMissingTablesAndColumns(tables: Array<Table>) {
        withTransaction {
            withDataBaseLock {
                SchemaUtils.createMissingTablesAndColumns(*tables)
            }
        }
    }
}
