package org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.repository

import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.BUSINESS_PARTNER_INDEX_NAME
import org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc.BusinessPartnerDoc
import org.eclipse.tractusx.bpdm.pool.config.OpenSearchConfigProperties
import org.eclipse.tractusx.bpdm.pool.exception.BpdmOpenSearchException
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.Refresh
import org.opensearch.client.opensearch.core.BulkRequest
import org.opensearch.client.opensearch.core.BulkResponse
import org.springframework.stereotype.Repository

@Repository("OpenSearchBusinessPartnerDocRepository")
class BusinessPartnerDocRepository(
    private val openSearchClient: OpenSearchClient,
    private val openSearchConfigProperties: OpenSearchConfigProperties
) {
    fun saveAll(businessPartnerDocs: Collection<BusinessPartnerDoc>) {
        if (businessPartnerDocs.isEmpty()) {
            return
        }

        val builder: BulkRequest.Builder = BulkRequest.Builder()

        for (businessPartnerDoc in businessPartnerDocs) {
            builder.operations { op ->
                op.index { idx ->
                    idx.index(BUSINESS_PARTNER_INDEX_NAME)
                        .id(businessPartnerDoc.bpn)
                        .document(businessPartnerDoc)
                }
            }

            if (openSearchConfigProperties.refreshOnWrite) {
                builder.refresh(Refresh.True)
            }
        }
        val result: BulkResponse = openSearchClient.bulk(builder.build())

        if (result.errors()) {
            val message = result.items().mapNotNull { it.error() }.mapNotNull { it.reason() }
                .joinToString(separator = "\n", prefix = "Error when saving business partner docs \n")
            throw BpdmOpenSearchException(message)
        }
    }
}