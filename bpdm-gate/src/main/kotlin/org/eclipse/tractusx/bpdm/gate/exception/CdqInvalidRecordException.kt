package org.eclipse.tractusx.bpdm.gate.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class CdqInvalidRecordException(
    cdqId: String?
) : RuntimeException("Business Partner with CDQ ID ${cdqId ?: "Unknown"} record in CDQ storage is invalid")