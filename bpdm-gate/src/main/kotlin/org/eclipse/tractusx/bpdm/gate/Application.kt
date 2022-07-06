package org.eclipse.tractusx.bpdm.gate

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication(
    scanBasePackages = [
        "org.eclipse.tractusx.bpdm.gate",
        "org.eclipse.tractusx.bpdm.common"
    ]
)
@ConfigurationPropertiesScan(
    basePackages = [
        "org.eclipse.tractusx.bpdm.gate",
        "org.eclipse.tractusx.bpdm.common"]
)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}

