package org.eclipse.tractusx.bpdm.pool.component.elastic.impl.doc

import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType

data class AddressDoc(
    @Field(type = FieldType.Nested)
    val administrativeAreas: Collection<TextDoc>,
    @Field(type = FieldType.Nested)
    val postCodes: Collection<TextDoc>,
    @Field(type = FieldType.Nested)
    val localities: Collection<TextDoc>,
    @Field(type = FieldType.Nested)
    val thoroughfares: Collection<TextDoc>,
    @Field(type = FieldType.Nested)
    val premises: Collection<TextDoc>,
    @Field(type = FieldType.Nested)
    val postalDeliveryPoints: Collection<TextDoc>
)
