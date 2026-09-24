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

package org.eclipse.tractusx.bpdm.pool.mapper.orchestrator.outbound

import org.eclipse.tractusx.bpdm.pool.model.error.AlternativeAlreadyAlternative
import org.eclipse.tractusx.bpdm.pool.model.error.AlternativeAlreadyMain
import org.eclipse.tractusx.bpdm.pool.model.error.AlternativeFlaggedUltimateOwner
import org.eclipse.tractusx.bpdm.pool.model.error.AlternativeHeadquarterUpsertParseError
import org.eclipse.tractusx.bpdm.pool.model.error.AlternativeNotFound
import org.eclipse.tractusx.bpdm.pool.model.error.AlternativeOfItself
import org.eclipse.tractusx.bpdm.pool.model.error.AlternativeOwned
import org.eclipse.tractusx.bpdm.pool.model.error.AlternativeOwns
import org.eclipse.tractusx.bpdm.pool.model.error.AlternativeRecordedInactive
import org.eclipse.tractusx.bpdm.pool.model.error.MainAlreadyAlternative
import org.eclipse.tractusx.bpdm.pool.model.error.MainNotFound
import org.eclipse.tractusx.bpdm.pool.model.error.MainRecordedInactive
import org.eclipse.tractusx.bpdm.pool.model.error.RelationReasonCodeNotFound
import org.eclipse.tractusx.bpdm.pool.model.error.RelationValidityPeriodParseError
import org.eclipse.tractusx.bpdm.pool.model.error.ReverseDesignationExists
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * Maps the sealed parse errors of an alternative headquarter designation to the error descriptions a golden record
 * relation task reports back.
 */
@Component
class AlternativeHeadquarterParseErrorMapper(
    private val validityPeriodErrorMapper: RelationValidityPeriodParseErrorMapper
) {

    /**
     * States why the designation was not written, naming the legal entity it faults.
     */
    fun toUpsertDescription(error: AlternativeHeadquarterUpsertParseError): String =
        when (error) {
            is AlternativeNotFound -> "No legal entity '${error.bpn}' to be designated an alternative headquarter"
            is MainNotFound -> "No legal entity '${error.bpn}' to be designated the main headquarter"
            is AlternativeOfItself -> "A legal entity cannot have a relation to itself (BPNL: ${error.bpn})."
            is RelationReasonCodeNotFound -> "Relation reason code '${error.reasonCode}' not found"
            is RelationValidityPeriodParseError -> validityPeriodErrorMapper.toUpsertDescription(error)
            is AlternativeRecordedInactive ->
                "Legal entity '${error.bpn}' is recorded as inactive during the validity period ${toPeriodDescription(error.validFrom, error.validTo)}, " +
                        "so it cannot be an alternative headquarter during that period"
            is MainRecordedInactive ->
                "Legal entity '${error.bpn}' is recorded as inactive during the validity period ${toPeriodDescription(error.validFrom, error.validTo)}, " +
                        "so it cannot be the main headquarter of an alternative during that period"
            is AlternativeAlreadyMain ->
                "Star topology violated: Legal entity '${error.alternativeBpn}' is already the main of an alternative relation " +
                        "with '${error.existingAlternativeBpn}' in an overlapping period, so it cannot also be an alternative"
            is AlternativeAlreadyAlternative ->
                "Star topology violated: Legal entity '${error.alternativeBpn}' is already alternative to " +
                        "'${error.existingMainBpn}' in an overlapping period, so it cannot also be alternative to another main"
            is MainAlreadyAlternative ->
                "Star topology violated: Legal entity '${error.mainBpn}' is already alternative to '${error.existingMainBpn}' " +
                        "in an overlapping period, so it cannot also be a main"
            is ReverseDesignationExists ->
                "Cannot reverse alternative headquarter relation: '${error.alternativeBpn}' cannot be alternative to " +
                        "'${error.mainBpn}' while the reverse relation exists in an overlapping period. End the original relation first."
            is AlternativeOwned ->
                "Invalid alternative headquarter relation: Legal entity '${error.alternativeBpn}' cannot be alternative because " +
                        "it participates in an overlapping IsOwnedBy relation (as owned entity with '${error.ownerBpn}')."
            is AlternativeOwns ->
                "Invalid alternative headquarter relation: Legal entity '${error.alternativeBpn}' cannot be alternative because " +
                        "it participates in an overlapping IsOwnedBy relation (as owning entity with '${error.ownedBpn}')."
            is AlternativeFlaggedUltimateOwner ->
                "Invalid alternative headquarter relation: Legal entity '${error.bpn}' cannot be alternative because it " +
                        "carries the ownershipUltimate flag."
        }

    private fun toPeriodDescription(validFrom: LocalDate, validTo: LocalDate?): String =
        "starting '$validFrom'${validTo?.let { " and ending '$it'" } ?: ""}"
}
