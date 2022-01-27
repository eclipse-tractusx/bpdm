package com.catenax.gpdm.controller.dto

import com.catenax.gpdm.entity.LocalityType
import com.catenax.gpdm.entity.NameType
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonUnwrapped

class NameDto (
    var type: NameType
        ){
    @JsonUnwrapped
    lateinit var nameComponent: BaseNamedDto

    constructor(nameComponent: BaseNamedDto, type: NameType): this(type){
        this.nameComponent=nameComponent
    }
}