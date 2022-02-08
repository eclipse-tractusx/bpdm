package com.catenax.gpdm.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
open class BpdmMultipleNotfound (
    objectType: String,
    identifiers: Collection<String>
    ):RuntimeException("$objectType with following identifiers not found: ${identifiers.joinToString()}")