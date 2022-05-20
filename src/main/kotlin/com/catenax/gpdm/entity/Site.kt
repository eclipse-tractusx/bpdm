package com.catenax.gpdm.entity

import javax.persistence.*

@Entity
@Table(name = "sites")
class Site(
    @Column(name = "bpn", nullable = false, unique = true)
    var bpn: String,
    @Column
    var name: String,
    @ManyToOne
    @JoinColumn(name = "partner_id", nullable = false)
    var partner: BusinessPartner,
    @OneToMany(mappedBy = "site", cascade = [CascadeType.ALL], orphanRemoval = true)
    val addresses: MutableSet<Address> = mutableSetOf()
) : BaseEntity() {
}