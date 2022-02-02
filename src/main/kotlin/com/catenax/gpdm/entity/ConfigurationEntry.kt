package com.catenax.gpdm.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "configuration_entries")
class ConfigurationEntry (
    @Column(name = "`key`", unique = true, nullable = false)
    var key: String,
    @Column(name = "`value`")
    var value: String?
        ) : BaseEntity()