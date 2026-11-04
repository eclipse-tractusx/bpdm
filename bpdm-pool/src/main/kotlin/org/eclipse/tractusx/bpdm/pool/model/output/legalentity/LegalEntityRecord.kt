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
package org.eclipse.tractusx.bpdm.pool.model.output.legalentity

import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityRelationTypeV7
import org.eclipse.tractusx.bpdm.pool.model.output.address.AddressRecord
import org.eclipse.tractusx.bpdm.pool.model.output.address.AddressScriptVariantRecord
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.BusinessStateRecord
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.ConfidenceCriteriaRecord
import org.eclipse.tractusx.bpdm.pool.model.output.businesspartner.RelationValidityPeriodRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.LegalFormRecord
import java.time.Instant

data class LegalEntityRecord(
    val bpn: String,
    val legalName: String,
    val legalShortName: String?,
    val legalForm: LegalFormRecord?,
    val identifiers: List<LegalEntityIdentifierRecord>,
    val states: List<BusinessStateRecord>,
    val relations: List<LegalEntityRelationRecord>,
    val currentness: Instant,
    val confidenceCriteria: ConfidenceCriteriaRecord,
    val isDataSpaceParticipant: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val ownershipUltimate: Boolean,
    val ultimateOwnerBpnl: String?,
    val legalAddress: AddressRecord,
    val scriptVariants: List<LegalEntityScriptVariantRecord>
)

data class LegalEntityIdentifierRecord(
    val value: String,
    val typeTechnicalKey: String,
    val typeName: String,
    val issuingBody: String?
)

data class LegalEntityRelationRecord(
    val type: LegalEntityRelationTypeV7,
    val sourceBpnl: String,
    val targetBpnl: String,
    val validityPeriods: List<RelationValidityPeriodRecord>,
    val reasonCode: String?
)

data class LegalEntityScriptVariantRecord(
    val scriptCode: String,
    val legalName: String,
    val shortName: String?,
    val legalAddress: AddressScriptVariantRecord
)
