package com.catenax.gpdm.dto.elastic

import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType

data class AddressDoc(
    @Field(type = FieldType.Search_As_You_Type)
    val administrativeAreas: Collection<String>,
    @Field(type = FieldType.Search_As_You_Type)
    val postCodes: Collection<String>,
    @Field(type = FieldType.Search_As_You_Type)
    val localities: Collection<String>,
    @Field(type = FieldType.Search_As_You_Type)
    val thoroughfares: Collection<String>,
    @Field(type = FieldType.Search_As_You_Type)
    val premises: Collection<String>,
    @Field(type = FieldType.Search_As_You_Type)
    val postalDeliveryPoints: Collection<String>
)
