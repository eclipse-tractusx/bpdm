package com.catenax.gpdm.entity

import java.util.*
import javax.persistence.*

@MappedSuperclass
abstract class BaseEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bpdm_sequence")
    @SequenceGenerator(name = "bpdm_sequence", sequenceName = "bpdm_sequence", allocationSize = 1)
    @Column(name = "id", nullable = false, updatable = false, insertable = false)
    val id: Long = 0,

    @Column(name = "uuid", nullable = false, updatable = false, unique = true, columnDefinition = "uuid")
    val uuid: UUID = UUID.randomUUID(),

    @Column(updatable = false, nullable = false, name = "CREATED_AT")
    val createdAt: Date = Date(),

    @Column(nullable = false, name = "UPDATED_AT")
    val updatedAt: Date = Date()
)