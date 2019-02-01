package customer

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import common.*
import mu.KotlinLogging
import java.io.File

class ServiceLocatorConfig {
    lateinit var ticket: ServiceAddress
}

@JsonIgnoreProperties(ignoreUnknown = true)
object CustomerConfig : HoconConfig {
    lateinit var db: DatabaseConfig
    lateinit var server: HttpServerConfig
    lateinit var serviceLocator: ServiceLocatorConfig
}

object CustomerDatabase : Database(CustomerConfig.db)

fun main(args: Array<String>) {
    CustomerConfig.load(args.getOrNull(0)?.let { File(it) })
    CustomerDatabase.createMissingTablesAndColumns(CustomerModel.tables())
    // start recovering uncompleted jobs
    CustomerTicketsService.recover()
    WebServer(CustomerConfig.server, CustomerController + JobController + CustomerTicketsController).start()
    KotlinLogging.logger("CustomerService").info { "up and running" }
}
