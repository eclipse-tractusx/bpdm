/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.model.raw.task

import org.eclipse.tractusx.bpdm.common.model.BusinessStateType
import org.eclipse.tractusx.bpdm.pool.model.raw.address.AddressScriptVariantRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.businesspartner.ConfidenceCriteriaRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.legalentity.LegalEntityIdentifierRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.legalentity.LegalEntityScriptVariantRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.address.LogisticAddressRaw
import org.eclipse.tractusx.bpdm.pool.model.raw.site.SiteScriptVariantRaw
import java.time.Instant

data class GoldenRecordTaskRaw(
    val taskId: String,
    val sharingMemberRecordId: String,
    val goldenRecord: GoldenRecordRaw,
    val passThrough: GoldenRecordPassThroughRaw
)

data class GoldenRecordRaw(
    val legalEntity: LegalEntityUpsertRaw,
    val site: SiteUpsertRaw?,
    val additionalAddress: AddressUpsertRaw?,
    val additionalSites: List<AdditionalSiteRaw>
)

/**
 * Whether an upsert writes a business partner that already exists, or only creates it when it does not.
 */
enum class UpsertIntent {
    AlwaysWrite,
    WriteOnlyIfAbsent
}

data class LegalEntityUpsertRaw(
    val reference: BpnReferenceRaw,
    val header: LegalEntityUpsertHeaderRaw,
    val legalAddress: AddressUpsertRaw,
    val intent: UpsertIntent
)

data class LegalEntityUpsertHeaderRaw(
    val legalName: String?,
    val legalShortName: String?,
    val legalForm: String?,
    val identifiers: List<LegalEntityIdentifierRaw>,
    val states: List<BusinessStateRaw>,
    val confidenceCriteria: ConfidenceCriteriaRaw,
    val isDataSpaceParticipant: Boolean?,
    val ownershipUltimate: Boolean?,
    val scriptVariants: List<LegalEntityScriptVariantRaw>
)

sealed interface SiteUpsertRaw {
    val reference: BpnReferenceRaw
    val header: SiteUpsertHeaderRaw
    val intent: UpsertIntent

    data class WithOwnMainAddress(
        override val reference: BpnReferenceRaw,
        override val header: SiteUpsertHeaderRaw,
        override val intent: UpsertIntent,
        val mainAddress: AddressUpsertRaw
    ) : SiteUpsertRaw

    data class WithLegalAddressAsMain(
        override val reference: BpnReferenceRaw,
        override val header: SiteUpsertHeaderRaw,
        override val intent: UpsertIntent,
        val mainAddressScriptVariants: List<AddressScriptVariantRaw>
    ) : SiteUpsertRaw
}

data class SiteUpsertHeaderRaw(
    val name: String?,
    val states: List<BusinessStateRaw>,
    val confidenceCriteria: ConfidenceCriteriaRaw,
    val scriptVariants: List<SiteScriptVariantRaw>
)

data class BusinessStateRaw(
    val validFrom: Instant?,
    val validTo: Instant?,
    val type: BusinessStateType?
)

data class AddressUpsertRaw(
    val reference: BpnReferenceRaw,
    val content: LogisticAddressRaw
)

data class AdditionalSiteRaw(
    val reference: BpnReferenceRaw,
    val name: String?
)

data class BpnReferenceRaw(
    val value: String?,
    val kind: BpnReferenceKind?
)

enum class BpnReferenceKind {
    Bpn,
    RequestIdentifier
}
