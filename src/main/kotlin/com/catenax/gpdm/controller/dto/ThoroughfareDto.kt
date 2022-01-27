package com.catenax.gpdm.controller.dto


import com.catenax.gpdm.entity.PremiseType
import com.catenax.gpdm.entity.ThoroughfareType
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonUnwrapped

class ThoroughfareDto (
        var type: ThoroughfareType
        ){
        @JsonUnwrapped
        lateinit var nameComponent: BaseNamedDto

        constructor(nameComponent: BaseNamedDto, type: ThoroughfareType): this(type){
                this.nameComponent=nameComponent
        }
}