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

package org.eclipse.tractusx.bpdm.pool.service.parser.relation

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.entity.ReasonCodeDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationValidityPeriodDb
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.model.error.PredecessorAndSuccessorIdentical
import org.eclipse.tractusx.bpdm.pool.model.error.PredecessorNotFound
import org.eclipse.tractusx.bpdm.pool.model.error.SuccessionCarriesEndDate
import org.eclipse.tractusx.bpdm.pool.model.error.SuccessionPartnerTypesDiffer
import org.eclipse.tractusx.bpdm.pool.model.error.SuccessionReasonCodeNotFound
import org.eclipse.tractusx.bpdm.pool.model.error.SuccessionUpsertParseError
import org.eclipse.tractusx.bpdm.pool.model.error.SuccessionValidityPeriodMissing
import org.eclipse.tractusx.bpdm.pool.model.error.SuccessionValidityPeriodsMultiple
import org.eclipse.tractusx.bpdm.pool.model.error.SuccessorNotFound
import org.eclipse.tractusx.bpdm.pool.model.parsed.SuccessionUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.request.SuccessionUpsertRequest
import org.eclipse.tractusx.bpdm.pool.model.request.SuccessionValidityPeriodRequest
import org.eclipse.tractusx.bpdm.pool.repository.ReasonCodeRepository
import org.eclipse.tractusx.bpdm.pool.service.parser.bpn.BpnTypeResolver
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Validates and resolves one succession, whichever kind of business partner it relates.
 *
 * Successions are parsed one at a time rather than as a batch: whether a predecessor already has a successor, and
 * whether a replacement closes a cycle, are questions about the succession graph, so an entry has to be judged against
 * the graph the entries before it left behind.
 */
@Service
class SuccessionUpsertParser(
    private val bpnTypeResolver: BpnTypeResolver,
    private val reasonCodeRepository: ReasonCodeRepository,
    private val legalEntitySuccessionParser: LegalEntitySuccessionParser,
    private val siteSuccessionParser: SiteSuccessionParser,
    private val addressSuccessionParser: AddressSuccessionParser
) {

    /**
     * Resolves what the succession names and reports every reason it cannot be written, among them two partners of
     * different kinds, a partner replacing itself, and a validity that is not the single open-ended period a
     * succession is.
     */
    @Transactional(readOnly = true)
    fun parse(request: SuccessionUpsertRequest): ParseResult<SuccessionUpsertParsed, SuccessionUpsertParseError> {
        val errors = mutableListOf<SuccessionUpsertParseError>()

        val partnerType = parsePartnerType(request, errors)
        val validityPeriod = parseValidityPeriod(request.validityPeriods, errors)
        val reasonCode = parseReasonCode(request.reasonCode, errors)

        if (partnerType == null || validityPeriod == null || errors.isNotEmpty()) return ParseResult.Failure(errors)

        return when (partnerType) {
            BusinessPartnerType.LEGAL_ENTITY -> legalEntitySuccessionParser.parse(request, validityPeriod, reasonCode)
            BusinessPartnerType.SITE -> siteSuccessionParser.parse(request, validityPeriod, reasonCode)
            BusinessPartnerType.ADDRESS -> addressSuccessionParser.parse(request, validityPeriod, reasonCode)
            BusinessPartnerType.GENERIC -> throw BpdmValidationException("No BPN this Pool issues names a generic business partner")
        }
    }

    private fun parsePartnerType(
        request: SuccessionUpsertRequest,
        errors: MutableList<SuccessionUpsertParseError>
    ): BusinessPartnerType? {
        val predecessorBpn = request.predecessorBpn.uppercase()
        val successorBpn = request.successorBpn.uppercase()

        val predecessorType = bpnTypeResolver.resolveType(predecessorBpn)
            ?: run { errors.add(PredecessorNotFound(request.predecessorBpn)); null }
        val successorType = bpnTypeResolver.resolveType(successorBpn)
            ?: run { errors.add(SuccessorNotFound(request.successorBpn)); null }

        if (predecessorBpn == successorBpn)
            errors.add(PredecessorAndSuccessorIdentical(request.predecessorBpn))

        if (predecessorType != null && successorType != null && predecessorType != successorType)
            errors.add(SuccessionPartnerTypesDiffer(request.predecessorBpn, request.successorBpn))

        return predecessorType?.takeIf { it == successorType }
    }

    private fun parseValidityPeriod(
        requests: List<SuccessionValidityPeriodRequest>,
        errors: MutableList<SuccessionUpsertParseError>
    ): RelationValidityPeriodDb? {
        val period = when (requests.size) {
            0 -> return run { errors.add(SuccessionValidityPeriodMissing); null }
            1 -> requests.single()
            else -> return run { errors.add(SuccessionValidityPeriodsMultiple); null }
        }

        period.validTo?.let { errors.add(SuccessionCarriesEndDate(it)) }

        return RelationValidityPeriodDb(validFrom = period.validFrom, validTo = null)
    }

    private fun parseReasonCode(reasonCode: String?, errors: MutableList<SuccessionUpsertParseError>): ReasonCodeDb? =
        reasonCode?.let {
            reasonCodeRepository.findByTechnicalKey(it) ?: run { errors.add(SuccessionReasonCodeNotFound(it)); null }
        }
}
