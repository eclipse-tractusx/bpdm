/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.sharingstate

data class PostSharingStatePendingV7Request(
    val externalIds: Collection<String>,
    val process: String
)
