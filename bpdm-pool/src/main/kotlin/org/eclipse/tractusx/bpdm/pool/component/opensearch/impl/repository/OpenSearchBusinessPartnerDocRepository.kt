package org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.repository

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.BusinessPartnerDoc
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch.core.BulkRequest
import org.opensearch.client.opensearch.core.BulkResponse
import org.springframework.stereotype.Service

@Service
class OpenSearchBusinessPartnerDocRepository(
    private val openSearchClient: OpenSearchClient
) {
    private val logger = KotlinLogging.logger { }

    fun saveAll(businessPartnerDocs: Collection<BusinessPartnerDoc>) {
        if (businessPartnerDocs.isEmpty()) {
            return
        }

        val builder: BulkRequest.Builder = BulkRequest.Builder()

        for (businessPartnerDoc in businessPartnerDocs) {
            builder.operations { op ->
                op.index { idx ->
                    idx.index("business-partner")
                        .id(businessPartnerDoc.bpn)
                        .document(businessPartnerDoc)
                }
            }
        }
        val result: BulkResponse = openSearchClient.bulk(builder.build())

        if (result.errors()) {
            //TODO: throw exception?
            logger.error("Error when saving business partner docs")
            for (item in result.items()) {
                if (item.error() != null) {
                    logger.error(item.error()!!.reason())
                }
            }
        }
    }
}