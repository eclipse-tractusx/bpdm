package org.eclipse.tractusx.bpdm.pool

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication


@SpringBootApplication(
    scanBasePackages = [
        "org.eclipse.tractusx.bpdm.pool.config",
        "org.eclipse.tractusx.bpdm.pool.controller",
        "org.eclipse.tractusx.bpdm.pool.repository",
        "org.eclipse.tractusx.bpdm.pool.service"
    ]
)
@ConfigurationPropertiesScan
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}