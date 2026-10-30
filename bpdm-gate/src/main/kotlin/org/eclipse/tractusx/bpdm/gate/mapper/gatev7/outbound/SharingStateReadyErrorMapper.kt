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

package org.eclipse.tractusx.bpdm.gate.mapper.gatev7.outbound

import org.eclipse.tractusx.bpdm.gate.model.parsed.sharingstate.SharingStateReadyError
import org.eclipse.tractusx.bpdm.gate.model.parsed.sharingstate.SharingStateReadyError.InvalidSharingState
import org.eclipse.tractusx.bpdm.gate.model.parsed.sharingstate.SharingStateReadyError.NotFound
import org.springframework.stereotype.Component

/**
 * Maps a rejected sharing state ready request to the error messages the API reports.
 */
@Component
class SharingStateReadyErrorMapper {

    /**
     * Returns the message the API reports for [error].
     */
    fun toMessage(error: SharingStateReadyError): String =
        when (error) {
            is NotFound ->
                "Business partner with external ID '${error.externalId}' not found"
            is InvalidSharingState ->
                "Business partner with external ID '${error.externalId}' is in sharing state ${error.sharingStateType}, " +
                        "but only business partners in an initial or error state can be set ready"
        }
}
