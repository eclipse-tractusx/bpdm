package com.catenax.gpdm.controller.dto

import com.catenax.gpdm.entity.NameType
import com.catenax.gpdm.entity.PostCodeType
import com.catenax.gpdm.entity.PremiseType
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonUnwrapped

class PremiseDto (
    var type: PremiseType
        ){
    @JsonUnwrapped
    lateinit var nameComponent: BaseNamedDto

    constructor(nameComponent: BaseNamedDto, type: PremiseType): this(type){
        this.nameComponent=nameComponent
    }
}