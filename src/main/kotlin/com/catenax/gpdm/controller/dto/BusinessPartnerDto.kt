package com.catenax.gpdm.controller.dto


import com.catenax.gpdm.entity.BusinessPartnerStatus
import com.catenax.gpdm.entity.BusinessPartnerTypes
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.fasterxml.jackson.annotation.JsonValue
import org.springframework.boot.autoconfigure.jackson.JacksonProperties

class BusinessPartnerDto (
    val bpn: String,
    val relations: Collection<RelationDto>
){
    @JsonUnwrapped
    lateinit var businessPartnerComponent: BusinessPartnerBaseDto

    constructor(bpn:String, businessPartnerComponent: BusinessPartnerBaseDto, relations:Collection<RelationDto>): this(bpn, relations){
        this.businessPartnerComponent=businessPartnerComponent
    }
}
