package payment

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("node")
class NodeProperties {
    var id: Int = 0
}
