package org.eclipse.tractusx.bpdm.pool.exception

import kotlin.reflect.KClass

class BpdmConversionException(
    private val objectDescription: String,
    private val objectType: KClass<*>,
    private val targetType: KClass<*>
): RuntimeException("Failed to convert object of type $objectType to $targetType. Object description: $objectDescription")