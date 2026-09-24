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

package org.eclipse.tractusx.bpdm.pool.model.error

import java.time.LocalDate

sealed interface AlternativeHeadquarterUpsertParseError

sealed interface DesignatedPartnerRecordedInactive : AlternativeHeadquarterUpsertParseError

data class AlternativeNotFound(val bpn: String) : AlternativeHeadquarterUpsertParseError

data class MainNotFound(val bpn: String) : AlternativeHeadquarterUpsertParseError

data class AlternativeOfItself(val bpn: String) : AlternativeHeadquarterUpsertParseError

data class AlternativeRecordedInactive(val bpn: String, val validFrom: LocalDate, val validTo: LocalDate?) :
    DesignatedPartnerRecordedInactive

data class MainRecordedInactive(val bpn: String, val validFrom: LocalDate, val validTo: LocalDate?) :
    DesignatedPartnerRecordedInactive

data class AlternativeAlreadyMain(val alternativeBpn: String, val existingAlternativeBpn: String) :
    AlternativeHeadquarterUpsertParseError

data class AlternativeAlreadyAlternative(val alternativeBpn: String, val existingMainBpn: String) :
    AlternativeHeadquarterUpsertParseError

data class MainAlreadyAlternative(val mainBpn: String, val existingMainBpn: String) : AlternativeHeadquarterUpsertParseError

data class ReverseDesignationExists(val alternativeBpn: String, val mainBpn: String) : AlternativeHeadquarterUpsertParseError

data class AlternativeOwned(val alternativeBpn: String, val ownerBpn: String) : AlternativeHeadquarterUpsertParseError

data class AlternativeOwns(val alternativeBpn: String, val ownedBpn: String) : AlternativeHeadquarterUpsertParseError

data class AlternativeFlaggedUltimateOwner(val bpn: String) : AlternativeHeadquarterUpsertParseError
