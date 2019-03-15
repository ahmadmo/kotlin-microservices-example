package transaction

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("node")
class NodeProperties {
    var id: Int = 0
}

@ConfigurationProperties("transaction")
class TransactionProperties {
    lateinit var timeout: Duration
}
