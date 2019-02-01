package common

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import io.vertx.core.json.Json
import mu.KotlinLogging
import java.io.File

interface HoconConfig {

    /**
     * Fills the fields of this config with a Hocon config.
     *
     * @param config to be loaded to this config
     */
    fun load(config: Config) {
        val json = config.root().render(ConfigRenderOptions.concise())
        Json.mapper.readerFor(javaClass).withValueToUpdate(this).readValue<HoconConfig>(json)
    }
}

fun HoconConfig.load(file: File? = null) {
    val logger = KotlinLogging.logger(javaClass.name)
    val config = if (file == null) {
        logger.info { "loading default config" }
        ConfigFactory.load()
    } else {
        logger.info { "loading config [file = $file]" }
        ConfigFactory.parseFile(file).resolve()
    }
    load(config)
}

// Common Config Classes

class DatabaseConfig {
    lateinit var url: String
    lateinit var user: String
    lateinit var pass: String
    lateinit var logLevel: String
}

class ServiceAddress {
    lateinit var host: String
    var port = 0
}

class HttpServerConfig {
    lateinit var bind: ServiceAddress
}
