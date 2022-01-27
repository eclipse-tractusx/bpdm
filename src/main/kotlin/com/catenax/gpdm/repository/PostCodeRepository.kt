package com.catenax.gpdm.repository

import com.catenax.gpdm.entity.AdministrativeArea
import com.catenax.gpdm.entity.PostCode
import org.springframework.data.repository.CrudRepository
import java.util.*

interface PostCodeRepository : CrudRepository<PostCode, Long> {

    fun findByUuidIn(uuids: Collection<UUID>): Set<PostCode>
}