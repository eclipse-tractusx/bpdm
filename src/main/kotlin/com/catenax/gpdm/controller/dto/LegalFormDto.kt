package com.catenax.gpdm.controller.dto

import com.catenax.gpdm.entity.ClassificationType
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonUnwrapped

class LegalFormDto (
    var type: String
        ){
    @JsonUnwrapped
    lateinit var nameComponent: BaseNamedDto

    constructor(nameComponent: BaseNamedDto, type: String): this(type){
        this.nameComponent=nameComponent
    }
}