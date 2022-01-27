package com.catenax.gpdm.controller.dto

import com.catenax.gpdm.entity.NameType
import com.catenax.gpdm.entity.PostalDeliveryPointType
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonUnwrapped

class PostalDeliveryPointDto (
        var type: PostalDeliveryPointType
        ){
        @JsonUnwrapped
        lateinit var nameComponent: BaseNamedDto

        constructor(nameComponent: BaseNamedDto, type: PostalDeliveryPointType): this(type){
                this.nameComponent=nameComponent
        }
}