package com.catenax.gpdm.exception

import com.catenax.gpdm.entity.SyncType
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.CONFLICT)
class BpdmSyncConflictException (
    val type: SyncType
        ): RuntimeException("Synchronization of type $type already running")