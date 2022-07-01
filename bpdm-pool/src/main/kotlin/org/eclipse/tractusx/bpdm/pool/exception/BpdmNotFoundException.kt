package org.eclipse.tractusx.bpdm.pool.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import kotlin.reflect.KClass

@ResponseStatus(HttpStatus.NOT_FOUND)
class BpdmNotFoundException (
    objectType: String,
    identifier: String
        ) : BpdmMultipleNotfound(objectType, listOf(identifier)){
            constructor(objectType: KClass<*>, identifier: String)
                    : this(objectType.simpleName ?: objectType.toString(), identifier)
        }