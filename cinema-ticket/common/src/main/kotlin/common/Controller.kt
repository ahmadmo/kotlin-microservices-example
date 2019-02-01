package common

import io.vertx.ext.web.Router

typealias RequestMapping = (Router).() -> Unit

operator fun RequestMapping.plus(that: RequestMapping): RequestMapping {
    val self = this
    return {
        val router = this
        self(router)
        that(router)
    }
}

open class Controller(val mapping: RequestMapping) {

    operator fun plus(that: Controller): Controller =
        Controller(this.mapping + that.mapping)
}
