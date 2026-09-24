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

package org.eclipse.tractusx.bpdm.pool.service.parser.relation

import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationValidityPeriodDb
import org.eclipse.tractusx.bpdm.pool.model.error.AlternativeRecordedInactive
import org.eclipse.tractusx.bpdm.pool.model.error.DesignatedPartnerRecordedInactive
import org.eclipse.tractusx.bpdm.pool.model.error.MainRecordedInactive
import org.eclipse.tractusx.bpdm.pool.model.toActivityTimeline
import org.springframework.stereotype.Service

/**
 * Checks that neither legal entity of an alternative headquarter designation is recorded as inactive while it holds.
 */
@Service
class AlternativeHeadquarterStateValidator {

    /**
     * Reports each of [validityPeriods] during which [alternative] or [main] is recorded as inactive on any day, once
     * for each of the two that is.
     */
    fun validate(
        alternative: LegalEntityDb,
        main: LegalEntityDb,
        validityPeriods: Collection<RelationValidityPeriodDb>
    ): List<DesignatedPartnerRecordedInactive> {
        val alternativeActivity = alternative.states.toActivityTimeline()
        val mainActivity = main.states.toActivityTimeline()

        return validityPeriods
            .sortedBy { it.validFrom }
            .flatMap { validityPeriod ->
                listOfNotNull(
                    if (alternativeActivity.isRecordedInactiveDuring(validityPeriod))
                        AlternativeRecordedInactive(alternative.bpn, validityPeriod.validFrom, validityPeriod.validTo)
                    else null,
                    if (mainActivity.isRecordedInactiveDuring(validityPeriod))
                        MainRecordedInactive(main.bpn, validityPeriod.validFrom, validityPeriod.validTo)
                    else null
                )
            }
    }
}
