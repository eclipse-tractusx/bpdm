/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.common.dto.saas

import java.time.LocalDateTime

data class BusinessPartnerSaas(
    val id: String? = null,
    val createdAt: LocalDateTime? = null,
    val lastModifiedAt: LocalDateTime? = null,
    val externalId: String? = null,
    val dataSource: String? = null,
    val disclosed: Boolean? = false,
    val updateMonitoring: Boolean? = false,
    val metadata: BusinessPartnerMetadataSaas? = null,
    val record: String? = null,
    val names: Collection<NameSaas> = emptyList(),
    val legalForm: LegalFormSaas? = null,
    val identifiers: Collection<IdentifierSaas> = emptyList(),
    val categories: Collection<TypeKeyNameUrlSaas> = emptyList(),
    val status: BusinessPartnerStatusSaas? = null,
    val profile: PartnerProfileSaas? = null,
    val relations: Collection<RelationSaas> = emptyList(),
    val types: Collection<TypeKeyNameUrlSaas> = emptyList(),
    val addresses: Collection<AddressSaas> = emptyList(),
    val bankAccounts: Collection<BankAccountSaas> = emptyList(),
)