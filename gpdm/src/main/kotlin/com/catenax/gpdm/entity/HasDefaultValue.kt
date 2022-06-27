package com.catenax.gpdm.entity

interface HasDefaultValue<T> {
    fun getDefault(): T
}