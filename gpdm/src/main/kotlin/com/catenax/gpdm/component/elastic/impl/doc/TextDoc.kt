package com.catenax.gpdm.component.elastic.impl.doc

import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType

data class TextDoc(
    @Field(type = FieldType.Search_As_You_Type)
    val text: String
)
