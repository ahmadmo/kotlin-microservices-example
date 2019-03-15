package account

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("node")
class NodeProperties {
    var id: Int = 0
}

@ConfigurationProperties("watcher")
class WatcherProperties {
    lateinit var linger: Duration
    lateinit var period: Duration
    var batchSize: Int = 0
}
