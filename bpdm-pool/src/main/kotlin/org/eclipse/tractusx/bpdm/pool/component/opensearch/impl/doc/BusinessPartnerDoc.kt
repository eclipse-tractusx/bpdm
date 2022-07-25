package org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc

data class BusinessPartnerDoc(
    val bpn: String,
    val names: Collection<TextDoc>,
    val legalForm: TextDoc?,
    val status: TextDoc?,
    val addresses: Collection<AddressDoc>,
    val classifications: Collection<TextDoc>,
    val sites: Collection<TextDoc>
)
