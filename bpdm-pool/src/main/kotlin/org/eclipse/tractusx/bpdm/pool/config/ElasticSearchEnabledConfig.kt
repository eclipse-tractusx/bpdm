package org.eclipse.tractusx.bpdm.pool.config

import org.eclipse.tractusx.bpdm.pool.component.elastic.impl.ElasticsearchImplConfig
import org.eclipse.tractusx.bpdm.pool.component.elastic.mock.ElasticsearchMockConfig
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.data.elasticsearch.ReactiveElasticsearchRestClientAutoConfiguration
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import


@Configuration
@Import(value = [
    ElasticsearchImplConfig::class,
    ElasticsearchRestClientAutoConfiguration::class,
    ElasticsearchDataAutoConfiguration::class,
    ElasticsearchRepositoriesAutoConfiguration::class,
    ReactiveElasticsearchRestClientAutoConfiguration::class])
@ConditionalOnProperty(
    value = ["bpdm.elastic.enabled"],
    havingValue = "true",
    matchIfMissing = false)
class ElasticSearchEnabledConfig

@Configuration
@Import(ElasticsearchMockConfig::class)
@ConditionalOnProperty(
    value = ["bpdm.elastic.enabled", "bpdm.opensearch.enabled"],
    havingValue = "false",
    matchIfMissing = true
)
class ElasticSearchDisabledConfig