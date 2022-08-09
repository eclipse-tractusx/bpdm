package org.eclipse.tractusx.bpdm.common.dto.cdq

data class PagedResponseCdq<T>(
    val limit: Int,
    val startAfter: String? = null,
    val nextStartAfter: String? = null,
    val total: Int,
    val values: Collection<T>
)
