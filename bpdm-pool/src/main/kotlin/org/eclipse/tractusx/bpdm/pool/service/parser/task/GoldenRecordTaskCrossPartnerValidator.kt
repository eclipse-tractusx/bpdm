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

package org.eclipse.tractusx.bpdm.pool.service.parser.task

import org.eclipse.tractusx.bpdm.pool.model.error.*
import org.eclipse.tractusx.bpdm.pool.model.parsed.AddressUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.LegalEntityUpsertParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.RecordAddressSitesParsed
import org.eclipse.tractusx.bpdm.pool.model.request.GoldenRecordTaskUpsertRequest
import org.eclipse.tractusx.bpdm.pool.model.request.RecordAddressSitesRequest
import org.eclipse.tractusx.bpdm.pool.model.request.SiteUpsertRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Reports where the partners one golden record upsert states contradict each other.
 *
 * Most of these rules read the partners as stated, but completeness of the additional sites is decidable only against
 * the address they will sit on, which is why the parsed partners are taken as well.
 */
@Service
class GoldenRecordTaskCrossPartnerValidator(
    private val bpnReferenceParser: BpnReferenceParser,
    private val sharedLegalAddressScriptCodeValidator: SharedLegalAddressScriptCodeValidator,
    private val taskAddressDistinctnessValidator: TaskAddressDistinctnessValidator,
    private val parentConsistencyValidator: GoldenRecordTaskParentConsistencyValidator,
    private val additionalSitesCompletenessValidator: AdditionalSitesCompletenessValidator
) {

    /**
     * Reports every contradiction between the partners [request] states and the partners parsed from it.
     */
    @Transactional(readOnly = true)
    fun validate(
        request: GoldenRecordTaskUpsertRequest,
        legalEntity: LegalEntityUpsertParsed?,
        recordAddressSites: RecordAddressSitesParsed?,
        additionalAddress: AddressUpsertParsed?
    ): List<CrossPartnerParseError> =
        sharedLegalAddressScriptCodeValidator.validate(request.legalEntity, request.sites.recordSite)
            .plus(validateStatedAddressDistinctness(request))
            .plus(validateAdditionalSitesHaveRecordSite(request.sites))
            .plus(parentConsistencyValidator.validate(request))
            .plus(additionalSitesCompletenessValidator.validate(legalEntity, recordAddressSites, additionalAddress))

    // The additional sites share the record address with the record's own site, so there has to be one.
    private fun validateAdditionalSitesHaveRecordSite(sites: RecordAddressSitesRequest): List<CrossPartnerParseError> =
        if (sites.recordSite == null && sites.additionalSites.isNotEmpty()) listOf(AdditionalSitesWithoutSite)
        else emptyList()

    private fun validateStatedAddressDistinctness(request: GoldenRecordTaskUpsertRequest): List<CrossPartnerParseError> =
        taskAddressDistinctnessValidator.validate(
            bpnReferenceParser.parse(request.legalEntity.legalAddress.reference),
            (request.sites.recordSite as? SiteUpsertRequest.WithOwnMainAddress)?.let { bpnReferenceParser.parse(it.mainAddress.reference) },
            request.additionalAddress?.let { bpnReferenceParser.parse(it.reference) }
        )
}
