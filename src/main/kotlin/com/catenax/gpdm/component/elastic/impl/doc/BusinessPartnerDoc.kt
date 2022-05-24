package com.catenax.gpdm.component.elastic.impl.doc

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType

@Document(indexName = "business-partner", createIndex = false)
data class BusinessPartnerDoc(
    @Id
    @ReadOnlyProperty
    val bpn: String,
    @Field(type = FieldType.Nested)
    val names: Collection<TextDoc>,
    @Field(type = FieldType.Nested)
    val legalForm: TextDoc?,
    @Field(type = FieldType.Nested)
    val status: TextDoc?,
    @Field(type = FieldType.Object)
    val addresses: Collection<AddressDoc>,
    @Field(type = FieldType.Nested)
    val classifications: Collection<TextDoc>,
    @Field(type = FieldType.Nested)
    val sites: Collection<TextDoc>
)
