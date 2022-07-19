package org.eclipse.tractusx.bpdm.pool.component.elastic.mock

import mu.KotlinLogging
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import javax.annotation.PostConstruct

@Configuration
@ComponentScan
class ElasticsearchMockConfig {
    private val logger = KotlinLogging.logger { }

    @PostConstruct
    fun logCreation() {
        logger.info { "Elasticsearch not enabled, mock connection" }
    }
}