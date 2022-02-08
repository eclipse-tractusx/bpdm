package com.catenax.gpdm.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
class BpdmNotFoundException (
    objectType: String,
    identifier: String
        ) : BpdmMultipleNotfound(objectType, listOf(identifier))