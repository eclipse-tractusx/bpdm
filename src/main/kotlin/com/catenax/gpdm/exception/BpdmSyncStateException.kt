package com.catenax.gpdm.exception

import com.catenax.gpdm.entity.SyncStatus
import com.catenax.gpdm.entity.SyncType

class BpdmSyncStateException (
    msg: String
): RuntimeException(msg)
