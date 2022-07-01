package org.eclipse.tractusx.bpdm.pool.entity

interface HasDefaultValue<T> {
    fun getDefault(): T
}