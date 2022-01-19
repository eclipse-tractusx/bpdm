package com.catenax.gpdm.controller.dto

import com.catenax.gpdm.entity.HardeningGrade
import com.catenax.gpdm.entity.IssuingAgency
import com.catenax.gpdm.entity.RegistrationStatus
import java.time.LocalDateTime
import javax.persistence.*

data class RegistrationDto (
    val hardeningGrade: HardeningGrade,
    val issuingAgency: BaseNamedDto,
    val status: RegistrationStatus,
    val initialRegistration: LocalDateTime,
    val lastUpdate: LocalDateTime
)