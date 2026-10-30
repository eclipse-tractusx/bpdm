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

package org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerRole
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.AdditionalSiteRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.AlternativeAddressRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.BusinessPartnerValuesRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.ConfidenceCriteriaRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.IdentifierRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.PhysicalPostalAddressRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.StateRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.AddressGoldenRecordRelationType
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.GoldenRecordRelationRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.LegalEntityGoldenRecordRelationType
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.SiteGoldenRecordRelationType
import java.time.Instant

data class BusinessPartnerOutputContentParsed(
    val nameParts: List<String>,
    val identifiers: Collection<IdentifierRaw>,
    val states: Collection<StateRaw>,
    val roles: Collection<BusinessPartnerRole>,
    val isOwnCompanyData: Boolean,
    val legalEntityBpn: String,
    val legalName: String?,
    val shortName: String?,
    val legalForm: String?,
    val siteBpn: String?,
    val siteName: String?,
    val addressBpn: String,
    val addressName: String?,
    val addressType: AddressType,
    val physicalPostalAddress: PhysicalPostalAddressRaw,
    val alternativePostalAddress: AlternativeAddressRaw?,
    val legalEntityConfidence: ConfidenceCriteriaRaw,
    val siteConfidence: ConfidenceCriteriaRaw?,
    val addressConfidence: ConfidenceCriteriaRaw,
    val legalEntityUpdatedAt: Instant?,
    val siteUpdatedAt: Instant?,
    val addressUpdatedAt: Instant?,
    val scriptVariants: List<BusinessPartnerValuesRaw.ScriptVariant>,
    val legalEntityGoldenRecordRelations: List<GoldenRecordRelationRaw<LegalEntityGoldenRecordRelationType>>,
    val siteGoldenRecordRelations: List<GoldenRecordRelationRaw<SiteGoldenRecordRelationType>>,
    val addressGoldenRecordRelations: List<GoldenRecordRelationRaw<AddressGoldenRecordRelationType>>,
    val ownershipUltimate: Boolean? = null,
    val ultimateOwnerBpnl: String? = null,
    val additionalSites: List<AdditionalSiteRaw> = emptyList()
)