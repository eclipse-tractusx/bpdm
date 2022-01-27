package com.catenax.gpdm.repository

import com.catenax.gpdm.entity.AdministrativeArea
import org.springframework.data.repository.CrudRepository
import java.util.*

interface AdministrativeAreaRepository: CrudRepository<AdministrativeArea, Long> {

    fun findByUuidIn(uuids: Collection<UUID>): Set<AdministrativeArea>
}