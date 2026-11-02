/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.common.model

import jakarta.persistence.*
import org.hibernate.annotations.Type
import java.util.UUID

/**
 * Supplies the technical identity and the creation and update timestamps of an entity.
 *
 * Nothing sets updatedAt automatically: whoever changes the entity sets it before saving.
 */
@MappedSuperclass
abstract class BaseEntityWithExplicitTimestamps(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bpdm_sequence")
    @SequenceGenerator(name = "bpdm_sequence", sequenceName = "bpdm_sequence", allocationSize = 1)
    @Column(name = "id", nullable = false, updatable = false, insertable = false)
    val id: Long = 0,

    @Column(name = "uuid", nullable = false, updatable = false, unique = true, columnDefinition = "uuid")
    val uuid: UUID = UUID.randomUUID(),

    @Column(updatable = false, nullable = false, name = "CREATED_AT")
    @Type(DbTimestampConverter::class)
    val createdAt: DbTimestamp = DbTimestamp.now(),

    @Column(nullable = false, name = "UPDATED_AT")
    @Type(DbTimestampConverter::class)
    var updatedAt: DbTimestamp = createdAt
)
