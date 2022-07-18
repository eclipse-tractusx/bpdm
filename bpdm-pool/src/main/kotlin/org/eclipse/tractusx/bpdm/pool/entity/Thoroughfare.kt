package org.eclipse.tractusx.bpdm.pool.entity

import com.neovisionaries.i18n.LanguageCode
import org.eclipse.tractusx.bpdm.common.model.ThoroughfareType
import javax.persistence.*

@Entity
@Table(name = "thoroughfares",
    indexes = [
        Index(columnList = "address_id")
    ])
class Thoroughfare (
    @Column(name = "`value`", nullable = false)
    val value: String,
    @Column(name = "name")
    val name: String?,
    @Column(name = "short_name")
    val shortName: String?,
    @Column(name = "number")
    val number: String?,
    @Column(name = "direction")
    val direction: String?,
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    val type: ThoroughfareType,
    @Column(name = "language", nullable = false)
    @Enumerated(EnumType.STRING)
    val language: LanguageCode,
    @ManyToOne
    @JoinColumn(name="address_id", nullable=false)
    var address: Address
        ) : BaseEntity()

