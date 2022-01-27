package com.catenax.gpdm.repository

import com.catenax.gpdm.entity.AddressVersion
import com.catenax.gpdm.entity.AdministrativeArea
import org.springframework.data.repository.CrudRepository
import java.util.*

interface AddressVersionRepository : CrudRepository<AddressVersion, Long> {

    fun findByUuidIn(uuids: Collection<UUID>): Set<AddressVersion>
}