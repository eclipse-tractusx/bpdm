package com.catenax.gpdm.controller.dto

import javax.persistence.Column

data class BaseNamedDto (
        val value: String,
        val shortName: String?,
        val number: Int?
        )