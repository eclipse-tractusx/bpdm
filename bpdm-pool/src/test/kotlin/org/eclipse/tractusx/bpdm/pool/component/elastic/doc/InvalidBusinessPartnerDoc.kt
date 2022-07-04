package org.eclipse.tractusx.bpdm.pool.component.elastic.doc

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType

@Document(indexName = "business-partner", createIndex = false)
data class InvalidBusinessPartnerDoc(
    @Id
    @ReadOnlyProperty
    val bpn: String,
    @Field(type = FieldType.Search_As_You_Type)
    val outdatedField: String
)
