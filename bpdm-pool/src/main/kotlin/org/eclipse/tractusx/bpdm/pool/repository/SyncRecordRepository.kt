package org.eclipse.tractusx.bpdm.pool.repository

import org.eclipse.tractusx.bpdm.pool.entity.SyncRecord
import org.eclipse.tractusx.bpdm.pool.entity.SyncType
import org.springframework.data.repository.CrudRepository

interface SyncRecordRepository: CrudRepository<SyncRecord, Long> {

    fun findByType(type: SyncType): SyncRecord?
}