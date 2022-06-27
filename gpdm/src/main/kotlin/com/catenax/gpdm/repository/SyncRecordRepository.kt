package com.catenax.gpdm.repository

import com.catenax.gpdm.entity.SyncRecord
import com.catenax.gpdm.entity.SyncType
import org.springframework.data.repository.CrudRepository

interface SyncRecordRepository: CrudRepository<SyncRecord, Long> {

    fun findByType(type: SyncType): SyncRecord?
}