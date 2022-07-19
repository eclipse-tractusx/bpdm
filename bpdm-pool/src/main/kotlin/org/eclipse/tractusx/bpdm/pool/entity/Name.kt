package org.eclipse.tractusx.bpdm.pool.entity

import com.neovisionaries.i18n.LanguageCode
import org.eclipse.tractusx.bpdm.common.model.NameType
import javax.persistence.*

@Entity
@Table(name = "names",
    indexes = [
        Index(columnList = "partner_id")
    ])
class Name(
    @Column(name = "`value`", nullable = false)
    val value: String,
    @Column(name = "shortName")
    val shortName: String?,
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    val type: NameType,
    @Column(name = "language", nullable = false)
    @Enumerated(EnumType.STRING)
    val language: LanguageCode,
    @ManyToOne
    @JoinColumn(name="partner_id", nullable=false)
    var partner: BusinessPartner
) : BaseEntity()

