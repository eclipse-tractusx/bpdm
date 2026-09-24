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

sealed interface SuccessionUpsertParseError

sealed interface LegalEntitySuccessionParseError : SuccessionUpsertParseError

sealed interface SiteSuccessionParseError : SuccessionUpsertParseError

sealed interface AddressSuccessionParseError : SuccessionUpsertParseError

/**
 * What a succession can be faulted for once the kind of business partner it relates is known.
 */
sealed interface SuccessionContentParseError :
    LegalEntitySuccessionParseError, SiteSuccessionParseError, AddressSuccessionParseError

data class PredecessorAndSuccessorIdentical(val bpn: String) : SuccessionUpsertParseError

data class SuccessionPartnerTypesDiffer(val predecessorBpn: String, val successorBpn: String) : SuccessionUpsertParseError

data object SuccessionValidityPeriodMissing : SuccessionUpsertParseError

data object SuccessionValidityPeriodsMultiple : SuccessionUpsertParseError

data class SuccessionCarriesEndDate(val validTo: LocalDate) : SuccessionUpsertParseError

data class SuccessionReasonCodeNotFound(val reasonCode: String) : SuccessionUpsertParseError

data class PredecessorNotFound(val bpn: String) : SuccessionContentParseError

data class SuccessorNotFound(val bpn: String) : SuccessionContentParseError

data class PredecessorRecordedActive(val bpn: String, val validFrom: LocalDate) : SuccessionContentParseError

data class SuccessorRecordedInactive(val bpn: String, val validFrom: LocalDate) : SuccessionContentParseError

data class PredecessorAlreadyReplaced(val predecessorBpn: String, val existingSuccessorBpn: String) : SuccessionContentParseError

data class SuccessionCycle(val predecessorBpn: String, val successorBpn: String) : SuccessionContentParseError

data class SuccessorInDifferentLegalEntity(val predecessorBpn: String, val successorBpn: String) :
    SiteSuccessionParseError, AddressSuccessionParseError
