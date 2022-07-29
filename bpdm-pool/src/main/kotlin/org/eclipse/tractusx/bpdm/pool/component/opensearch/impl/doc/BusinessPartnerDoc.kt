package org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc

import com.fasterxml.jackson.annotation.JsonIgnore

const val BUSINESS_PARTNER_INDEX_NAME = "business-partner"

data class BusinessPartnerDoc(
    @JsonIgnore // ignore since this is the id and does not need to be in the document source
    val bpn: String,
    val names: Collection<TextDoc>,
    val legalForm: TextDoc?,
    val status: TextDoc?,
    val addresses: Collection<AddressDoc>,
    val classifications: Collection<TextDoc>,
    val sites: Collection<TextDoc>
)
