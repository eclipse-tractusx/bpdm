package org.eclipse.tractusx.bpdm.common.model

interface HasDefaultValue<T> {
    fun getDefault(): T
}