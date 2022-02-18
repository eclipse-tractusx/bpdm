package com.catenax.gpdm.repository.entity

import com.catenax.gpdm.entity.Identifier
import org.springframework.data.repository.CrudRepository
import java.util.*

interface IdentifierRepository : CrudRepository<Identifier, Long> {
    fun findByUuidIn(uuid: Collection<UUID>): Set<Identifier>
}