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

package org.eclipse.tractusx.bpdm.common.dto

import com.neovisionaries.i18n.CurrencyCode
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Bank Account", description = "Bank account record of a business partner")
data class BankAccountDto(
    @Schema(description = "Trust scores for the account", defaultValue = "[]")
    val trustScores: Collection<Float> = emptyList(),
    @Schema(description = "Used currency in the account", defaultValue = "UNDEFINED")
    val currency: CurrencyCode = CurrencyCode.UNDEFINED,
    @Schema(description = "ID used to identify this account internationally")
    val internationalBankAccountIdentifier: String,
    @Schema(description = "ID used to identify the account's bank internationally")
    val internationalBankIdentifier: String,
    @Schema(description = "ID used to identify the account domestically")
    val nationalBankAccountIdentifier: String,
    @Schema(description = "ID used to identify the account's bank domestically")
    val nationalBankIdentifier: String,
)
