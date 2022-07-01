package org.eclipse.tractusx.bpdm.pool.entity

import com.neovisionaries.i18n.LanguageCode
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.persistence.EnumType
import javax.persistence.Enumerated


@Embeddable
class AddressVersion(
    @Enumerated(EnumType.STRING)
    @Column(name = "character_set", nullable = false)
    val characterSet: CharacterSet,
    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false)
    val language: LanguageCode
)

enum class CharacterSet(private val typeName: String) : NamedType, HasDefaultValue<CharacterSet> {
    ARABIC("Arabic"),
    CHINESE("Simplified Chinese"),
    CHINESE_TRADITIONAL("Traditional Chinese"),
    CYRILLIC("Cyrillic"),
    GREEK("Greek"),
    HANGUL_KOREAN("Hangul"),
    HEBREW("Hebrew"),
    HIRAGANA("Hiragana"),
    KANJI("Kanji"),
    KATAKANA("Katakana"),
    LATIN("Latin"),
    THAI("Thai"),
    WESTERN_LATIN_STANDARD("Western Latin Standard (ISO 8859-1; Latin-1)"),
    UNDEFINED("Undefined");

    override fun getTypeName(): String {
        return typeName
    }

    override fun getDefault(): CharacterSet {
        return UNDEFINED
    }
}