package org.eclipse.tractusx.bpdm.pool.config

import org.eclipse.tractusx.bpdm.pool.component.elastic.mock.ElasticsearchMockConfig
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.OpenSearchImplConfig
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import


@Configuration
@Import(value = [OpenSearchImplConfig::class])
@ConditionalOnProperty(
    value = ["bpdm.opensearch.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class OpenSearchEnabledConfig

@Configuration
@Import(ElasticsearchMockConfig::class)
@ConditionalOnProperty(
    value = ["bpdm.opensearch.enabled", "bpdm.elastic.enabled"],
    havingValue = "false",
    matchIfMissing = true
)
class OpenSearchDisabledConfig