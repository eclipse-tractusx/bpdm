package com.catenax.gpdm.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.CONFLICT)
class BpdmAlreadyExists (
    objectType: String,
    identifier: String
):RuntimeException("$objectType with the following identifier already exists: $identifier")