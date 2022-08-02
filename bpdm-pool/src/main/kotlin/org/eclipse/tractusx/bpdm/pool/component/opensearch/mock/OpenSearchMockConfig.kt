package org.eclipse.tractusx.bpdm.pool.component.opensearch.mock

import mu.KotlinLogging
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import javax.annotation.PostConstruct

@Configuration
@ComponentScan
class OpenSearchMockConfig {
    private val logger = KotlinLogging.logger { }

    @PostConstruct
    fun logCreation() {
        logger.info { "OpenSearch not enabled, mock connection" }
    }
}