package org.eclipse.tractusx.bpdm.pool.entity

import javax.persistence.*

@Entity
@Table(name = "sites")
class Site(
    @Column(name = "bpn", nullable = false, unique = true)
    var bpn: String,
    @Column(name = "name", nullable = false)
    var name: String,
    @ManyToOne
    @JoinColumn(name = "partner_id", nullable = false)
    var partner: BusinessPartner,
    @OneToMany(mappedBy = "site", cascade = [CascadeType.ALL], orphanRemoval = true)
    val addresses: MutableSet<Address> = mutableSetOf()
) : BaseEntity() {
}