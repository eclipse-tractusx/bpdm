package com.catenax.gpdm.controller.dto

import com.catenax.gpdm.entity.LocalityType
import com.catenax.gpdm.entity.NameType
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonUnwrapped

class LocalityDto (
    var type: LocalityType
        ){
    @JsonUnwrapped
    lateinit var nameComponent: BaseNamedDto

    constructor(nameComponent: BaseNamedDto, type: LocalityType): this(type){
        this.nameComponent=nameComponent
    }
}