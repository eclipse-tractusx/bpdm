package org.eclipse.tractusx.bpdm.common.model

enum class PostCodeType(private val codeName: String, private val url: String) : NamedUrlType, HasDefaultValue<PostCodeType> {
    CEDEX("Courrier d’Entreprise à Distribution Exceptionnelle", ""),
    LARGE_MAIL_USER("Large mail user", ""),
    OTHER("Other type", ""),
    POST_BOX("Post Box", ""),
    REGULAR("Regular", "");

    override fun getTypeName(): String {
        return codeName
    }

    override fun getUrl(): String {
        return url
    }

    override fun getDefault(): PostCodeType {
        return OTHER
    }
}