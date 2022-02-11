package com.catenax.gpdm.dto.request

import com.catenax.gpdm.entity.ClassificationType

data class ClassificationRequest (
        val value: String,
        val code: String?,
        val type: ClassificationType?
        )