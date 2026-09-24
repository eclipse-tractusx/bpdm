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

import org.eclipse.tractusx.bpdm.pool.model.error.PredecessorAlreadyReplaced
import org.eclipse.tractusx.bpdm.pool.model.error.PredecessorAndSuccessorIdentical
import org.eclipse.tractusx.bpdm.pool.model.error.PredecessorNotFound
import org.eclipse.tractusx.bpdm.pool.model.error.PredecessorRecordedActive
import org.eclipse.tractusx.bpdm.pool.model.error.SuccessionCarriesEndDate
import org.eclipse.tractusx.bpdm.pool.model.error.SuccessionCycle
import org.eclipse.tractusx.bpdm.pool.model.error.SuccessionPartnerTypesDiffer
import org.eclipse.tractusx.bpdm.pool.model.error.SuccessionReasonCodeNotFound
import org.eclipse.tractusx.bpdm.pool.model.error.SuccessionUpsertParseError
import org.eclipse.tractusx.bpdm.pool.model.error.SuccessionValidityPeriodMissing
import org.eclipse.tractusx.bpdm.pool.model.error.SuccessionValidityPeriodsMultiple
import org.eclipse.tractusx.bpdm.pool.model.error.SuccessorInDifferentLegalEntity
import org.eclipse.tractusx.bpdm.pool.model.error.SuccessorNotFound
import org.eclipse.tractusx.bpdm.pool.model.error.SuccessorRecordedInactive
import org.springframework.stereotype.Component

/**
 * Maps the sealed parse errors of a succession to the error descriptions a golden record relation task reports back.
 *
 * The `when` is exhaustive so a new error won't compile until it gets a description.
 */
@Component
class SuccessionParseErrorMapper {

    /**
     * States why the succession was not written, naming the partner it faults.
     */
    fun toUpsertDescription(error: SuccessionUpsertParseError): String =
        when (error) {
            is PredecessorNotFound -> "No business partner '${error.bpn}' to be replaced"
            is SuccessorNotFound -> "No business partner '${error.bpn}' to replace it"
            is PredecessorAndSuccessorIdentical -> "Business partner '${error.bpn}' cannot replace itself"
            is SuccessionPartnerTypesDiffer ->
                "A succession relates two business partners of the same kind, but '${error.predecessorBpn}' and " +
                        "'${error.successorBpn}' are of different kinds"
            SuccessionValidityPeriodMissing -> "A succession states the date it starts on"
            SuccessionValidityPeriodsMultiple ->
                "A succession states one validity period; correct a wrong date by restating that one period"
            is SuccessionCarriesEndDate ->
                "A succession does not end, but this one states '${error.validTo}' as its end date"
            is SuccessionReasonCodeNotFound -> "Relation reason code '${error.reasonCode}' not found"
            is PredecessorRecordedActive ->
                "Business partner '${error.bpn}' is still recorded as active on or after '${error.validFrom}', so it " +
                        "cannot be replaced from that date on"
            is SuccessorRecordedInactive ->
                "Business partner '${error.bpn}' is recorded as inactive on '${error.validFrom}', so it cannot take " +
                        "another partner's place from that date on"
            is PredecessorAlreadyReplaced ->
                "Business partner '${error.predecessorBpn}' is already replaced by '${error.existingSuccessorBpn}' in " +
                        "an overlapping validity period"
            is SuccessionCycle ->
                "Business partner '${error.predecessorBpn}' is already replacing '${error.successorBpn}', so it cannot " +
                        "be replaced by it"
            is SuccessorInDifferentLegalEntity ->
                "Business partners '${error.predecessorBpn}' and '${error.successorBpn}' belong to different legal " +
                        "entities, so neither replaces the other"
        }
}
