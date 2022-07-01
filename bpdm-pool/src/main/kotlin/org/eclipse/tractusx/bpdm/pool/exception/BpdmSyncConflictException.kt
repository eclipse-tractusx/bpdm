package org.eclipse.tractusx.bpdm.pool.exception

import org.eclipse.tractusx.bpdm.pool.entity.SyncType
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.CONFLICT)
class BpdmSyncConflictException (
    val type: SyncType
        ): RuntimeException("Synchronization of type $type already running")