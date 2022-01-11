package com.catenax.gpdm.entity

import com.neovisionaries.i18n.LanguageCode
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "address_versions")
class AddressVersion (
    @Column(name = "character_set", nullable = false)
    val characterSet: CharacterSet,
    @Column(name = "language", nullable = false)
    val languageCode: LanguageCode
        ) : BaseEntity()

enum class CharacterSet(val description: String){
    ARABIC("Arabic characters, e.g., س, ش, ص, ض, ط."),
    CHINESE("Simplified Chinese characters, e.g. 令, 青, 卬, 印, 熙."),
    CHINESE_TRADITIONAL("Traditional Chinese characters, e.g., 廠, 廣, 業, 鄉, 氣."),
    CYRILLIC("Cyrillic characters, e.g., Ф, Ц, ж, к, ш."),
    GREEK("Greek characters, e.g., Δ, Ψ, Π, δ, ϊ."),
    HANGUL_KOREAN("Characters from the Korean alphabet (Hangul), e.g., ㄹ, ㅕ, ㄾ, ㅖ, ㅠ."),
    HEBREW("Hebrew characters, e.g., א, ב, ג, ד, ה."),
    HIRAGANA("Japanese characters, e.g., あ, い, う, え, お."),
    KANJI("Japanese characters, e.g., 明, 極, 輸, 清, 兵."),
    KATAKANA("Japanese characters, e.g., ア, ギ, ヌ, テ, ヤ."),
    LATIN("Latin alphabet, without any special characters."),
    THAI("Thai characters, e.g., ร, หั, ส, สำ, ห."),
    WESTERN_LATIN_STANDARD("Western Latin Standard (ISO 8859-1; Latin-1), contains special western characters, e.g., ä, ö, ü, ß."),
}