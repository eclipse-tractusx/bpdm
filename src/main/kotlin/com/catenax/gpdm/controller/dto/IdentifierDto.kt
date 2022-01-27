package com.catenax.gpdm.controller.dto

import com.catenax.gpdm.entity.ClassificationType
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonUnwrapped

class IdentifierDto (
        var type: String,
        var registration: RegistrationDto?
        ){
        @JsonUnwrapped
        lateinit var nameComponent: BaseNamedDto

        constructor(nameComponent: BaseNamedDto, type: String, registration: RegistrationDto?): this(type, registration){
                this.nameComponent=nameComponent
        }
}