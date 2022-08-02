package org.eclipse.tractusx.bpdm.pool.component.opensearch.impl

import mu.KotlinLogging
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import javax.annotation.PostConstruct

@Configuration
@ComponentScan
@EnableAsync
class OpenSearchImplConfig {
    private val logger = KotlinLogging.logger { }

    @PostConstruct
    fun logCreation() {
        logger.info { "Enable and configure OpenSearch connection" }
    }
}