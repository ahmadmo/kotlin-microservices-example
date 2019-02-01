package ticket

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import common.*
import mu.KotlinLogging
import java.io.File

@JsonIgnoreProperties(ignoreUnknown = true)
object TicketConfig : HoconConfig {
    lateinit var db: DatabaseConfig
    lateinit var server: HttpServerConfig
}

object TicketDatabase : Database(TicketConfig.db)

fun main(args: Array<String>) {
    TicketConfig.load(args.getOrNull(0)?.let { File(it) })
    TicketDatabase.createMissingTablesAndColumns(TicketModel.tables())
    WebServer(TicketConfig.server, TicketController).start()
    KotlinLogging.logger("TicketService").info { "up and running" }
}
