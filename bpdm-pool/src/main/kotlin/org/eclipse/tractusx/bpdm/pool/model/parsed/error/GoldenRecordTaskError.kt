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

package org.eclipse.tractusx.bpdm.pool.model.parsed.error

/**
 * A reason a golden record task cannot be carried out, named after the business partner of the task it concerns.
 */
sealed interface GoldenRecordTaskError {

    data object StateTypeMissing : GoldenRecordTaskError

    data class UnchangedLegalEntityNotFound(val bpn: String) : GoldenRecordTaskError

    data class LegalEntityCreateInvalid(val error: LegalEntityCreateError) : GoldenRecordTaskError

    data class LegalEntityUpdateInvalid(val error: LegalEntityUpdateError) : GoldenRecordTaskError

    data class UnchangedSiteNotFound(val bpn: String) : GoldenRecordTaskError

    data object SiteNotInLegalEntity : GoldenRecordTaskError

    data object SiteMainAddressNotInLegalEntity : GoldenRecordTaskError

    data object SiteMainAddressIsLegalAddress : GoldenRecordTaskError

    data class SiteCreateInvalid(val error: SiteCreateError) : GoldenRecordTaskError

    data class SiteUpdateInvalid(val error: SiteUpdateError) : GoldenRecordTaskError

    data object AdditionalAddressNotInLegalEntity : GoldenRecordTaskError

    data class AdditionalAddressCreateInvalid(val error: AddressContentError) : GoldenRecordTaskError

    data class AdditionalAddressUpdateInvalid(val error: AddressUpdateError) : GoldenRecordTaskError

    data class AdditionalSiteCreateInvalid(val error: SiteContentError) : GoldenRecordTaskError

    data class AdditionalSiteInvalid(val error: AddressSiteMembershipError) : GoldenRecordTaskError

    data class AdditionalSiteOfOtherLegalEntity(val siteBpn: String) : GoldenRecordTaskError
}
