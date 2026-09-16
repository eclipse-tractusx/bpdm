/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/


package org.eclipse.tractusx.bpdm.pool.service.parser.task

import org.eclipse.tractusx.bpdm.pool.model.error.AdditionalAddressRestatesLegalAddress
import org.eclipse.tractusx.bpdm.pool.model.error.AdditionalAddressRestatesSiteMainAddress
import org.eclipse.tractusx.bpdm.pool.model.error.StatedAddressDistinctnessParseError
import org.eclipse.tractusx.bpdm.pool.model.error.SiteMainAddressRestatesLegalAddress
import org.eclipse.tractusx.bpdm.pool.model.parsed.BpnReferenceParsed
import org.springframework.stereotype.Service

/**
 * The rule that the legal, site main and additional address a request states are three different addresses.
 *
 * Naming one of them twice leaves the Pool to guess which of the two payloads the one row should end up with. Where
 * a site's main address really is the legal address, the request says so by stating no main address of its own.
 */
@Service
class StatedAddressDistinctnessValidator {

    /**
     * Reports every pair of stated addresses that turn out to be one address.
     */
    fun validate(
        legalAddress: BpnReferenceParsed,
        siteMainAddress: BpnReferenceParsed?,
        additionalAddress: BpnReferenceParsed?
    ): List<StatedAddressDistinctnessParseError> =
        listOfNotNull(
            SiteMainAddressRestatesLegalAddress.takeIf { namesSameRecord(legalAddress, siteMainAddress) },
            AdditionalAddressRestatesLegalAddress.takeIf { namesSameRecord(legalAddress, additionalAddress) },
            AdditionalAddressRestatesSiteMainAddress.takeIf { namesSameRecord(siteMainAddress, additionalAddress) }
        )

    // Two references that name nothing yet name two records to be created, however alike they look.
    private fun namesSameRecord(one: BpnReferenceParsed?, other: BpnReferenceParsed?): Boolean =
        when {
            one is BpnReferenceParsed.Existing && other is BpnReferenceParsed.Existing -> one.bpn == other.bpn
            one is BpnReferenceParsed.Pending && other is BpnReferenceParsed.Pending ->
                one.requestIdentifier == other.requestIdentifier
            else -> false
        }
}
