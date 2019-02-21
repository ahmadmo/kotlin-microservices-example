package edge

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.netflix.hystrix.EnableHystrix
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication
@EnableDiscoveryClient
@EnableHystrix
@EnableFeignClients
class EdgeServiceApplication

fun main(args: Array<String>) {
    runApplication<EdgeServiceApplication>(*args)
}
