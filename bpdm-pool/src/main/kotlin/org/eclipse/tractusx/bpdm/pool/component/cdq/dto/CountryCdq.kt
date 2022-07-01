package org.eclipse.tractusx.bpdm.pool.component.cdq.dto

import com.neovisionaries.i18n.CountryCode

data class CountryCdq(
    val shortName: CountryCode?,
    val value: String?
)
