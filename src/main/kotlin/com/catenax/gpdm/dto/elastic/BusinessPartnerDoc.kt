package com.catenax.gpdm.dto.elastic

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType

@Document(indexName = "business-partner", createIndex = true)
data class BusinessPartnerDoc(
    @Id
    val bpn: String,
    @Field(type = FieldType.Search_As_You_Type)
    val names: Collection<String>,
    @Field(type = FieldType.Search_As_You_Type)
    val legalForm: String?,
    @Field(type = FieldType.Search_As_You_Type)
    val status: String?,
    @Field(type = FieldType.Object)
    val addresses: Collection<AddressDoc>,
    @Field(type = FieldType.Search_As_You_Type)
    val classifications: Collection<String>,
)
