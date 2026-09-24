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

package org.eclipse.tractusx.bpdm.pool.mapper.shared.outbound

import org.eclipse.tractusx.bpdm.pool.model.error.AddressStateRelationParseError
import org.eclipse.tractusx.bpdm.pool.model.error.AlternativeHeadquarterRecordedInactive
import org.eclipse.tractusx.bpdm.pool.model.error.LegalEntityStateRelationParseError
import org.eclipse.tractusx.bpdm.pool.model.error.MainHeadquarterRecordedInactive
import org.eclipse.tractusx.bpdm.pool.model.error.ManagerRecordedInactive
import org.eclipse.tractusx.bpdm.pool.model.error.OwnerRecordedInactive
import org.eclipse.tractusx.bpdm.pool.model.error.ReplacedLegalEntityRecordedActive
import org.eclipse.tractusx.bpdm.pool.model.error.ReplacingLegalEntityRecordedInactive
import org.eclipse.tractusx.bpdm.pool.model.error.SiteStateRelationParseError
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * Maps the errors of states that contradict a partner's relations to the descriptions every API reports them with.
 *
 * Each description opens with the relation and its counterpart, because a golden record task cuts its error text short.
 */
@Component
class StateRelationParseErrorMapper {

    /**
     * States which relation of the legal entity its states contradict.
     */
    fun toDescription(error: LegalEntityStateRelationParseError): String =
        when (error) {
            is ReplacedLegalEntityRecordedActive -> toReplacedDescription(error.successorBpn, error.validFrom, LEGAL_ENTITY, error.bpn)
            is ReplacingLegalEntityRecordedInactive -> toReplacingDescription(error.predecessorBpn, error.validFrom, LEGAL_ENTITY, error.bpn)
            is OwnerRecordedInactive ->
                toGoverningDescription("Owner of '${error.ownedBpn}'", error.validFrom, error.validTo, error.bpn)
            is ManagerRecordedInactive ->
                toGoverningDescription("Data manager of '${error.managedBpn}'", error.validFrom, error.validTo, error.bpn)
            is AlternativeHeadquarterRecordedInactive ->
                toGoverningDescription("Alternative headquarter for '${error.mainBpn}'", error.validFrom, error.validTo, error.bpn)
            is MainHeadquarterRecordedInactive ->
                toGoverningDescription("Main headquarter of alternative '${error.alternativeBpn}'", error.validFrom, error.validTo, error.bpn)
        }

    /**
     * States which succession of the site its states contradict.
     */
    fun toDescription(error: SiteStateRelationParseError): String =
        when (error) {
            is SiteStateRelationParseError.ReplacedSiteRecordedActive ->
                toReplacedDescription(error.successorBpn, error.validFrom, SITE, error.bpn)
            is SiteStateRelationParseError.ReplacingSiteRecordedInactive ->
                toReplacingDescription(error.predecessorBpn, error.validFrom, SITE, error.bpn)
        }

    /**
     * States which succession of the address its states contradict.
     */
    fun toDescription(error: AddressStateRelationParseError): String =
        when (error) {
            is AddressStateRelationParseError.ReplacedAddressRecordedActive ->
                toReplacedDescription(error.successorBpn, error.validFrom, ADDRESS, error.bpn)
            is AddressStateRelationParseError.ReplacingAddressRecordedInactive ->
                toReplacingDescription(error.predecessorBpn, error.validFrom, ADDRESS, error.bpn)
        }

    private fun toReplacedDescription(successorBpn: String, validFrom: LocalDate, partnerKind: String, bpn: String) =
        "Replaced by '$successorBpn' from '$validFrom': $partnerKind '$bpn' cannot be recorded as active on or after that date"

    private fun toReplacingDescription(predecessorBpn: String, validFrom: LocalDate, partnerKind: String, bpn: String) =
        "Replacing '$predecessorBpn' from '$validFrom': $partnerKind '$bpn' cannot be recorded as inactive on that date"

    private fun toGoverningDescription(role: String, validFrom: LocalDate, validTo: LocalDate?, bpn: String): String {
        val period = if (validTo == null) "from '$validFrom' on" else "from '$validFrom' until '$validTo'"
        return "$role $period: $LEGAL_ENTITY '$bpn' cannot be recorded as inactive during that period"
    }

    private companion object {
        const val LEGAL_ENTITY = "legal entity"
        const val SITE = "site"
        const val ADDRESS = "address"
    }
}
