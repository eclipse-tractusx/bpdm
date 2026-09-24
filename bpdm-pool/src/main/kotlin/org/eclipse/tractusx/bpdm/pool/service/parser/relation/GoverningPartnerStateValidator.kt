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
import org.eclipse.tractusx.bpdm.pool.model.error.GoverningPartnerRecordedInactive
import org.eclipse.tractusx.bpdm.pool.model.toActivityTimeline
import org.springframework.stereotype.Service

/**
 * Checks that the legal entity governing another, as its owner or its data manager, is not recorded as inactive while
 * it governs.
 *
 * Only the governing side is judged: an owned or managed legal entity may go out of use while the relation holds.
 */
@Service
class GoverningPartnerStateValidator {

    /**
     * Reports each of [validityPeriods] during which [governingPartner] is recorded as inactive on any day.
     */
    fun validate(
        governingPartner: LegalEntityDb,
        validityPeriods: Collection<RelationValidityPeriodDb>
    ): List<GoverningPartnerRecordedInactive> {
        val activity = governingPartner.states.toActivityTimeline()

        return validityPeriods
            .sortedBy { it.validFrom }
            .filter { activity.isRecordedInactiveDuring(it) }
            .map { GoverningPartnerRecordedInactive(governingPartner.bpn, it.validFrom, it.validTo) }
    }
}
