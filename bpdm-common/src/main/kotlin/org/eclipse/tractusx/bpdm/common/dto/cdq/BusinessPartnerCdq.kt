/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.common.dto.cdq

import java.time.LocalDateTime

data class BusinessPartnerCdq(
    val id: String? = null,
    val createdAt: LocalDateTime? = null,
    val lastModifiedAt: LocalDateTime? = null,
    val externalId: String? = null,
    val dataSource: String? = null,
    val disclosed: Boolean? = false,
    val updateMonitoring: Boolean? = false,
    val metadata: BusinessPartnerMetadataCdq? = null,
    val record: String? = null,
    val names: Collection<NameCdq> = emptyList(),
    val legalForm: LegalFormCdq? = null,
    val identifiers: Collection<IdentifierCdq> = emptyList(),
    val categories: Collection<TypeKeyNameUrlCdq> = emptyList(),
    val status: BusinessPartnerStatusCdq? = null,
    val profile: PartnerProfileCdq? = null,
    val relations: Collection<RelationCdq> = emptyList(),
    val types: Collection<TypeKeyNameUrlCdq> = emptyList(),
    val addresses: Collection<AddressCdq> = emptyList(),
    val bankAccounts: Collection<BankAccountCdq> = emptyList(),
)