package com.catenax.gpdm.repository

import com.catenax.gpdm.entity.IssuingAgency
import org.springframework.data.repository.CrudRepository

interface IssuingAgencyRepository: CrudRepository<IssuingAgency, Long> {

    fun findAllByValueIn(values: Set<String>): Set<IssuingAgency>
}