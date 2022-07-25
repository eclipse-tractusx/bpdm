package org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc

data class AddressDoc(
    val administrativeAreas: Collection<TextDoc>,
    val postCodes: Collection<TextDoc>,
    val localities: Collection<TextDoc>,
    val thoroughfares: Collection<TextDoc>,
    val premises: Collection<TextDoc>,
    val postalDeliveryPoints: Collection<TextDoc>
)
