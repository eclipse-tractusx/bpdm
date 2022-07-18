package org.eclipse.tractusx.bpdm.pool.entity

import com.neovisionaries.i18n.LanguageCode
import org.eclipse.tractusx.bpdm.common.model.LocalityType
import javax.persistence.*

@Entity
@Table(name = "localities",
    indexes = [
        Index(columnList = "address_id")
    ])
class Locality (
    @Column(name = "`value`", nullable = false)
    val value: String,
    @Column(name = "short_name")
    val shortName: String?,
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    val localityType: LocalityType,
    @Column(name = "language", nullable = false)
    @Enumerated(EnumType.STRING)
    val language: LanguageCode,
    @ManyToOne
    @JoinColumn(name = "address_id", nullable = false)
    var address: Address
        ) : BaseEntity()

