package org.eclipse.tractusx.bpdm.pool.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import kotlin.reflect.KClass

@ResponseStatus(HttpStatus.NOT_FOUND)
open class BpdmMultipleNotfound (
    objectType: String,
    identifiers: Collection<String>
    ):RuntimeException("$objectType with following identifiers not found: ${identifiers.joinToString()}"){
        constructor(objectType: KClass<*>, identifiers: Collection<String>):
                this(objectType.simpleName?: objectType.toString(), identifiers)
    }