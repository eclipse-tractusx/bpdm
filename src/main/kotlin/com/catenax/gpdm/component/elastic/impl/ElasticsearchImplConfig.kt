package com.catenax.gpdm.component.elastic.impl

import com.catenax.gpdm.config.ElasticSearchConfigProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories
import org.springframework.scheduling.annotation.EnableAsync

@Configuration
@ComponentScan
@EnableAsync
@EnableElasticsearchRepositories(basePackages = ["com.catenax.gpdm.component.elastic.impl.repository"])
class ElasticsearchImplConfig(
    val esProperties: ElasticSearchConfigProperties
)