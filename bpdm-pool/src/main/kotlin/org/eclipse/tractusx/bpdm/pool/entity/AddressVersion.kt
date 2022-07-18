package org.eclipse.tractusx.bpdm.pool.entity

import com.neovisionaries.i18n.LanguageCode
import org.eclipse.tractusx.bpdm.common.model.CharacterSet
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

