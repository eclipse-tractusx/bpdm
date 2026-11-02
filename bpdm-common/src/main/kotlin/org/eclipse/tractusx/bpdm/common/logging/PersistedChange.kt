/*
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
 */

package org.eclipse.tractusx.bpdm.common.logging

/**
 * A change to one aspect of a subject that outlives its transaction.
 */
interface PersistedChange {
    val type: PersistedChangeType
    val subject: PersistedChangeSubject

    fun identifierForLog(): String

    fun aspectForLog(): String?
}

enum class PersistedChangeType {
    Created,
    Updated,
    Deleted
}

data class PersistedChangeSubject(
    val singular: String,
    val plural: String
)
