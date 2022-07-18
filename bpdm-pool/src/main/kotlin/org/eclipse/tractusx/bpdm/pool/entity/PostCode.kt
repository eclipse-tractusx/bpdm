package org.eclipse.tractusx.bpdm.pool.entity

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.common.model.PostCodeType
import javax.persistence.*

@Entity
@Table(name = "post_codes",
    indexes = [
        Index(columnList = "address_id")
    ])
class PostCode (
    @Column(name = "`value`", nullable = false)
    val value: String,
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    val type: PostCodeType,
    @Column(name = "country", nullable = false)
    @Enumerated(EnumType.STRING)
    val countryCode: CountryCode,
    @ManyToOne
    @JoinColumn(name = "address_id", nullable = false)
    var address: Address
        ) : BaseEntity()

