package org.eclipse.tractusx.bpdm.common.service

import com.neovisionaries.i18n.CountryCode
import com.neovisionaries.i18n.CurrencyCode
import com.neovisionaries.i18n.LanguageCode
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameUrlDto
import org.eclipse.tractusx.bpdm.common.model.NamedType
import org.eclipse.tractusx.bpdm.common.model.NamedUrlType

fun <T : NamedUrlType> T.toDto(): TypeKeyNameUrlDto<T> {
    return TypeKeyNameUrlDto(this, getTypeName(), getUrl())
}

fun <T : NamedType> T.toDto(): TypeKeyNameDto<T> {
    return TypeKeyNameDto(this, getTypeName())
}

fun LanguageCode.toDto(): TypeKeyNameDto<LanguageCode> {
    return TypeKeyNameDto(this, getName())
}

fun CountryCode.toDto(): TypeKeyNameDto<CountryCode> {
    return TypeKeyNameDto(this, getName())
}

fun CurrencyCode.toDto(): TypeKeyNameDto<CurrencyCode> {
    return TypeKeyNameDto(this, getName())
}