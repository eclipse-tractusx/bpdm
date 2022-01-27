package com.catenax.gpdm.controller.dto

import com.catenax.gpdm.entity.PostCodeType
import com.catenax.gpdm.entity.PostalDeliveryPointType
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonUnwrapped

class PostCodeDto (
    var type: PostCodeType
        ){
    @JsonUnwrapped
    lateinit var nameComponent: BaseNamedDto

    constructor(nameComponent: BaseNamedDto, type: PostCodeType): this(type){
        this.nameComponent=nameComponent
    }
}