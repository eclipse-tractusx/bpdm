package org.eclipse.tractusx.bpdm.common.dto.cdq

import com.neovisionaries.i18n.CountryCode

data class CountryCdq(
    val shortName: CountryCode?,
    val value: String? = null
)
