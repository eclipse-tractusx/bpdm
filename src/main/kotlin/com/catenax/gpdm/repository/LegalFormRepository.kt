package com.catenax.gpdm.repository

import com.catenax.gpdm.entity.IssuingAgency
import com.catenax.gpdm.entity.LegalForm
import org.springframework.data.repository.CrudRepository

interface LegalFormRepository : CrudRepository<LegalForm, Long> {
    fun findAllByValueIn(values: Set<String>): Set<LegalForm>
}