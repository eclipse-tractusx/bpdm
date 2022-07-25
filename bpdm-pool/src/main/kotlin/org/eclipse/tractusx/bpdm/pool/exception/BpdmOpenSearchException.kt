package org.eclipse.tractusx.bpdm.pool.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class BpdmOpenSearchException(message: String) : RuntimeException(message) {
}