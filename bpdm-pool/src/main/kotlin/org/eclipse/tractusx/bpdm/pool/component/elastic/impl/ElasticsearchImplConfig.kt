package org.eclipse.tractusx.bpdm.pool.component.elastic.impl

import mu.KotlinLogging
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories
import org.springframework.scheduling.annotation.EnableAsync
import javax.annotation.PostConstruct

@Configuration
@ComponentScan
@EnableAsync
@EnableElasticsearchRepositories(basePackages = ["org.eclipse.tractusx.bpdm.pool.component.elastic.impl.repository"])
class ElasticsearchImplConfig {
    private val logger = KotlinLogging.logger { }

    @PostConstruct
    fun logCreation() {
        logger.info { "Enable and configure Elasticsearch connection" }
    }
}