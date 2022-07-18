package org.eclipse.tractusx.bpdm.pool.entity

import com.neovisionaries.i18n.LanguageCode
import org.eclipse.tractusx.bpdm.common.model.PremiseType
import javax.persistence.*

@Entity
@Table(name = "premises",
    indexes = [
        Index(columnList = "address_id")
    ])
class Premise(
    @Column(name = "`value`", nullable = false)
    val value: String,
    @Column(name = "short_name")
    val shortName: String?,
    @Column(name = "number")
    val number: String?,
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    val type: PremiseType,
    @Column(name = "language", nullable = false)
    @Enumerated(EnumType.STRING)
    val language: LanguageCode,
    @ManyToOne
    @JoinColumn(name="address_id", nullable=false)
    var address: Address
) : BaseEntity()

