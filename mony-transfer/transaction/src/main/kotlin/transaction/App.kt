package transaction

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient

@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(NodeProperties::class, TransactionProperties::class)
class TransactionServiceApplication

fun main(args: Array<String>) {
    runApplication<TransactionServiceApplication>(*args)
}
