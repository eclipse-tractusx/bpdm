package org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.service

import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.mapping.TypeMapping
import org.opensearch.client.opensearch.indices.CreateIndexRequest
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.util.ResourceUtils

@Service
class OpenSearchStarterService(
    val openSearchClient: OpenSearchClient
) {
    @EventListener(ContextRefreshedEvent::class)
    fun createOnInit() {
        val indexAlreadyExists = openSearchClient.indices().exists { it.index("business-partner") }.value()

        if (!indexAlreadyExists) {
            val indexFile = ResourceUtils.getFile("classpath:opensearch/index-mappings.json")
            val jsonpMapper = openSearchClient._transport().jsonpMapper()
            val jsonParser = jsonpMapper.jsonProvider().createParser(indexFile.inputStream())
            val createIndexRequest =
                CreateIndexRequest.Builder().index("business-partner").mappings(TypeMapping._DESERIALIZER.deserialize(jsonParser, jsonpMapper)).build()
            openSearchClient.indices().create(createIndexRequest)
        }
    }
}