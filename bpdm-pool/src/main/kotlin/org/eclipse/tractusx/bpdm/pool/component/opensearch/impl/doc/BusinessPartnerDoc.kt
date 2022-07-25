package org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc

const val BUSINESS_PARTNER_INDEX_NAME = "business-partner"

data class BusinessPartnerDoc(
    val bpn: String,
    val names: Collection<TextDoc>,
    val legalForm: TextDoc?,
    val status: TextDoc?,
    val addresses: Collection<AddressDoc>,
    val classifications: Collection<TextDoc>,
    val sites: Collection<TextDoc>
)
