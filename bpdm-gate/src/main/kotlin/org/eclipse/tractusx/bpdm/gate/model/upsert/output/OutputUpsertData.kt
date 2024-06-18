/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.gate.model.upsert.output

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerRole

data class OutputUpsertData(
    val nameParts: List<String>,
    val identifiers: Collection<Identifier>,
    val states: Collection<State>,
    val roles: Collection<BusinessPartnerRole>,
    val isOwnCompanyData: Boolean,
    val legalEntityBpn: String,
    val legalName: String,
    val shortName: String?,
    val legalForm: String?,
    val siteBpn: String?,
    val siteName: String?,
    val addressBpn: String,
    val addressName: String?,
    val addressType: AddressType,
    val physicalPostalAddress: PhysicalPostalAddress,
    val alternativePostalAddress: AlternativeAddress?,
    val legalEntityConfidence: ConfidenceCriteria,
    val siteConfidence: ConfidenceCriteria?,
    val addressConfidence: ConfidenceCriteria,
)