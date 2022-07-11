package org.eclipse.tractusx.bpdm.gate.service

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class CdqRequestException(message: String, cause: Throwable) : RuntimeException(message, cause)