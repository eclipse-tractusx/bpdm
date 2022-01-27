package com.catenax.gpdm.controller.dto

import com.catenax.gpdm.entity.ClassificationType
import com.catenax.gpdm.entity.NameType
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonUnwrapped

class ClassificationDto (
        val type: ClassificationType
        ){
        @JsonUnwrapped
        lateinit var nameComponent: BaseNamedDto

        constructor(nameComponent: BaseNamedDto, type: ClassificationType): this(type){
                this.nameComponent=nameComponent
        }
}