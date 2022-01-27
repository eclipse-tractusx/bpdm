package com.catenax.gpdm.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
class BpdmNotFoundException (
    val objectType: String,
    val identifier: String
        ):RuntimeException("$objectType with identifier '$identifier' not found.")