package org.eclipse.tractusx.bpdm.common.model

enum class ThoroughfareType(private val typeName: String, private val url: String) : NamedUrlType, HasDefaultValue<ThoroughfareType> {
    INDUSTRIAL_ZONE("An industrial zone", ""),
    OTHER("Other type", ""),
    RIVER("River", ""),
    SQUARE("Square", ""),
    STREET("Street", "");

    override fun getTypeName(): String {
        return typeName
    }

    override fun getUrl(): String {
        return url
    }

    override fun getDefault(): ThoroughfareType {
        return OTHER
    }
}