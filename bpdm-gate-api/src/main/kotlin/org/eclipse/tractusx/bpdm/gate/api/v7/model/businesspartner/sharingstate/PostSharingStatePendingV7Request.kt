/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.sharingstate

import io.swagger.v3.oas.annotations.media.Schema

data class PostSharingStatePendingV7Request(
    @get:Schema(description = "The external IDs of the business partners to mark as picked up")
    val externalIds: Collection<String>,

    @get:Schema(description = "The alternative refinement process the caller works on")
    val process: String
)
