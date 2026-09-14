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

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.model.BpnReferenceAllocation
import org.eclipse.tractusx.bpdm.pool.model.PartnerScriptCodes
import org.eclipse.tractusx.bpdm.pool.model.error.*
import org.eclipse.tractusx.bpdm.pool.model.parsed.*
import org.eclipse.tractusx.bpdm.pool.model.request.*
import org.eclipse.tractusx.bpdm.pool.service.parser.ScriptVariantCoverageValidator
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressBpnParser
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressContentParser
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AlternativeHeadquarterValidator
import org.eclipse.tractusx.bpdm.pool.service.parser.legalentity.LegalEntityBpnParser
import org.eclipse.tractusx.bpdm.pool.service.parser.legalentity.LegalEntityHeaderParser
import org.eclipse.tractusx.bpdm.pool.service.parser.legalentity.LegalEntityIdentifierDuplicateValidator
import org.eclipse.tractusx.bpdm.pool.service.parser.legalentity.UltimateOwnerUniquenessValidator
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteBpnParser
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteMainAddressConsistencyValidator
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteHeaderParser
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteLegalEntityConsistencyValidator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteCreateWithReferencedAddressAsMainParser
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteCreateWithLegalAddressAsMainParser
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteCreateParser
import org.eclipse.tractusx.bpdm.pool.service.parser.legalentity.LegalEntityCreateParser
import org.eclipse.tractusx.bpdm.pool.service.parser.address.TypedParentAddressCreateParser
import org.eclipse.tractusx.bpdm.pool.service.parser.site.SiteUpdateParser
import org.eclipse.tractusx.bpdm.pool.service.parser.legalentity.LegalEntityUpdateParser
import org.eclipse.tractusx.bpdm.pool.service.parser.address.AddressUpdateParser
import org.eclipse.tractusx.bpdm.pool.model.request.SiteUpdateRequest
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityUpdateRequest
import org.eclipse.tractusx.bpdm.pool.model.request.AddressUpdateRequest

/**
 * Turns a golden record upsert request into the plan of what it will do, or into every reason it cannot be done.
 *
 * One request at a time, not a batch: entries of one reservation may name the same request identifier and must then
 * reach the same record, which is only known once the earlier entry has been written. The request's own content is
 * still parsed through the batch-shaped content parsers, one entry's worth at a time.
 */
@Service
class GoldenRecordUpsertParser(
    private val legalEntityUpsertParser: LegalEntityUpsertParser,
    private val siteUpsertParser: SiteUpsertParser,
    private val additionalAddressUpsertParser: AdditionalAddressUpsertParser,
    private val siteReferenceParser: SiteReferenceParser,
    private val referenceResolutionParser: BpnReferenceResolutionParser,
    private val siteHeaderParser: SiteHeaderParser,
    private val siteMainAddressConsistencyValidator: SiteMainAddressConsistencyValidator,
    private val sharedLegalAddressScriptCodeValidator: SharedLegalAddressScriptCodeValidator,
    private val siteLegalEntityConsistencyValidator: SiteLegalEntityConsistencyValidator,
    private val statedAddressDistinctnessValidator: StatedAddressDistinctnessValidator,
    private val coverageWriteReader: GoldenRecordCoverageWriteReader,
    private val coverageValidator: ScriptVariantCoverageValidator,
    private val parentConsistencyValidator: GoldenRecordParentConsistencyValidator
) {

    /**
     * Reports the plan [request] amounts to, or every problem that stops it.
     */
    @Transactional(readOnly = true)
    fun parse(
        request: GoldenRecordUpsertRequest,
        bpnReferences: BpnReferenceAllocation
    ): ParseResult<GoldenRecordUpsertParsed, GoldenRecordUpsertParseError> {
        val errors = mutableListOf<GoldenRecordUpsertParseError>()

        val legalEntity = legalEntityUpsertParser.parse(request.legalEntity, bpnReferences).recordIn(errors)
        val site = request.site?.let { siteUpsertParser.parse(it, request.legalEntity, bpnReferences).recordIn(errors) }
        val additionalAddress = request.additionalAddress?.let { additionalAddressUpsertParser.parse(it, bpnReferences).recordIn(errors) }
        val membership = request.site?.let { parseMembership(request, bpnReferences, errors) }

        errors += sharedLegalAddressScriptCodeValidator.validate(request.legalEntity, request.site)
        errors += statedAddressDistinctnessValidator.validate(
            referenceResolutionParser.parse(request.legalEntity.legalAddress.reference, bpnReferences),
            (request.site as? SiteUpsertRequest.WithOwnMainAddress)?.let { referenceResolutionParser.parse(it.mainAddress.reference, bpnReferences) },
            request.additionalAddress?.let { referenceResolutionParser.parse(it.reference, bpnReferences) }
        )
        errors += parentConsistencyValidator.validate(request, bpnReferences)
        errors += membershipOmissions(legalEntity, site, additionalAddress, membership)
        errors += foreignMembershipSites(legalEntity, membership)

        // Coverage reads the partners a written address is shared with. A partner this request failed to resolve is
        // not among them as far as the check can tell, so it would report the request taking away coverage it never
        // had. The other cross-partner checks resolve what they need themselves and stay meaningful.
        if (errors.none { it is LegalEntityNotFound || it is SiteNotFound || it is SiteMainAddressNotFound || it is AdditionalAddressNotFound })
            errors += coverageValidator.validate(listOf(coverageWriteReader.writesOf(request, bpnReferences)))
                .single()
                .map { ScriptVariantCoverageLost(it) }

        if (errors.isNotEmpty()) return ParseResult.Failure(errors)

        return ParseResult.Success(plan(request.sharingMemberRecordId, legalEntity!!, site, additionalAddress, membership))
    }

    /**
     * The plan variant the request amounts to: which kind of business partner it is about, and under which parent.
     *
     * Every plan the request states has been parsed successfully by the time this runs, so a partner the request
     * names has a plan and one it does not name has none.
     */
    private fun plan(
        sharingMemberRecordId: String,
        legalEntity: LegalEntityUpsertPlan,
        site: SiteUpsertPlan?,
        additionalAddress: AddressUpsertPlan?,
        membership: SiteMembershipPlan?
    ): GoldenRecordUpsertParsed =
        when {
            additionalAddress != null && site != null ->
                GoldenRecordUpsertParsed.SiteAddressRecord(sharingMemberRecordId, legalEntity, site, additionalAddress, membership!!)
            additionalAddress != null ->
                GoldenRecordUpsertParsed.LegalEntityAddressRecord(sharingMemberRecordId, legalEntity, additionalAddress)
            site != null ->
                GoldenRecordUpsertParsed.SiteRecord(sharingMemberRecordId, legalEntity, site, membership!!)
            else ->
                GoldenRecordUpsertParsed.LegalEntityRecord(sharingMemberRecordId, legalEntity)
        }

    private fun parseMembership(
        request: GoldenRecordUpsertRequest,
        bpnReferences: BpnReferenceAllocation,
        errors: MutableList<GoldenRecordUpsertParseError>
    ): SiteMembershipPlan {
        // The same site stated twice is one statement written twice, not two memberships. An entry is identified by
        // the reference it carries and, carrying none, by the name its site is to be created under.
        val statedOnce = request.addressSiteMembership.distinctBy { it.reference.value ?: it.name }
        val confidence = membershipSiteConfidence(request)

        val existingSites = mutableListOf<SiteDb>()
        val newSites = mutableListOf<MembershipSiteCreatePlan>()

        statedOnce.forEachIndexed { index, stated ->
            when (val result = siteReferenceParser.parse(stated.reference, bpnReferences)) {
                is ParseResult.Failure -> errors += result.errors.map { toMembershipError(index, it) }
                is ParseResult.Success -> {
                    val resolved = result.parsed
                    val site = resolved.target
                    if (site != null) {
                        existingSites += site
                    } else {
                        // A membership entry naming no site yet asks for one to be created on the record's address.
                        val header = single(
                            siteHeaderParser.parse(listOf(SiteHeaderRequest(stated.name, emptyList(), confidence, emptyList()))),
                            errors
                        ) { MembershipSiteContentInvalid(index, it) }
                        if (header != null) newSites += MembershipSiteCreatePlan(resolved.reference, header)
                    }
                }
            }
        }

        return SiteMembershipPlan(existingSites, newSites)
    }

    /**
     * The stated membership sites that belong to another legal entity. The address they would be linked to is this
     * request's own, so a site outside its legal entity can never be a member of it - and one this request creates
     * its legal entity for has no members that could already exist.
     */
    private fun foreignMembershipSites(
        legalEntity: LegalEntityUpsertPlan?,
        membership: SiteMembershipPlan?
    ): List<GoldenRecordUpsertParseError> {
        if (membership == null) return emptyList()
        val legalEntityTarget = legalEntityTarget(legalEntity)
            // A legal entity this request creates has no sites yet, so every site already persisted is another's.
            ?: return membership.existingSites.map { MembershipSiteNotInLegalEntity(it.bpn, null) }

        return membership.existingSites
            .flatMap { siteLegalEntityConsistencyValidator.check(legalEntityTarget, it) }
            .map { MembershipSiteNotInLegalEntity(it.siteBpn, it.legalEntityBpn) }
    }

    private fun legalEntityTarget(plan: LegalEntityUpsertPlan?): LegalEntityDb? =
        when (plan) {
            is LegalEntityUpsertPlan.Unchanged -> plan.target
            is LegalEntityUpsertPlan.Update -> plan.target
            is LegalEntityUpsertPlan.Create, null -> null
        }


    /**
     * The sites the record address is already the main address of that the membership does not state. Only decidable
     * where that address exists: one this request creates cannot be another site's main address yet.
     */
    private fun membershipOmissions(
        legalEntity: LegalEntityUpsertPlan?,
        site: SiteUpsertPlan?,
        additionalAddress: AddressUpsertPlan?,
        membership: SiteMembershipPlan?
    ): List<GoldenRecordUpsertParseError> {
        if (site == null || membership == null) return emptyList()
        val recordAddress = recordAddressTarget(legalEntity, site, additionalAddress) ?: return emptyList()

        // The record's own site is always part of the membership it states, whether or not the request repeats it.
        val statedSites = membership.existingSites.plus(listOfNotNull(siteTarget(site)))
        return siteMainAddressConsistencyValidator.check(recordAddress, statedSites)
            .map { MembershipOmitsSiteMainAddress(it.siteBpn) }
    }

    private fun siteTarget(site: SiteUpsertPlan): SiteDb? =
        when (site) {
            is SiteUpsertPlan.Unchanged -> site.target
            is SiteUpsertPlan.UpdateWithOwnMainAddress -> site.target
            is SiteUpsertPlan.UpdateOnLegalAddress -> site.parsed.target
            is SiteUpsertPlan.CreateWithOwnMainAddress,
            is SiteUpsertPlan.CreateOnLegalAddress,
            is SiteUpsertPlan.CreateOnExistingAddress -> null
        }

    /** The already persisted address this request is about, where it has one: the deepest address it states. */
    private fun recordAddressTarget(
        legalEntity: LegalEntityUpsertPlan?,
        site: SiteUpsertPlan?,
        additionalAddress: AddressUpsertPlan?
    ): LogisticAddressDb? =
        (additionalAddress as? AddressUpsertPlan.Update)?.target
            ?: siteMainAddressTarget(site)
            ?: legalAddressTarget(legalEntity)

    private fun siteMainAddressTarget(site: SiteUpsertPlan?): LogisticAddressDb? =
        when (site) {
            is SiteUpsertPlan.Unchanged -> site.target.mainAddress
            is SiteUpsertPlan.CreateOnExistingAddress -> site.parsed.mainAddress
            is SiteUpsertPlan.UpdateWithOwnMainAddress -> site.target.mainAddress
            is SiteUpsertPlan.UpdateOnLegalAddress -> site.parsed.target.mainAddress
            is SiteUpsertPlan.CreateWithOwnMainAddress, is SiteUpsertPlan.CreateOnLegalAddress, null -> null
        }

    private fun legalAddressTarget(legalEntity: LegalEntityUpsertPlan?): LogisticAddressDb? =
        legalEntityTarget(legalEntity)?.legalAddress

    /**
     * A site created through the membership list has had no confidence assessed for it of its own, so it borrows the
     * assessment the request carries for the business partner it is about.
     */
    private fun membershipSiteConfidence(request: GoldenRecordUpsertRequest): ConfidenceCriteriaRequest =
        request.site?.header?.confidenceCriteria
            ?: request.additionalAddress?.content?.confidenceCriteria
            ?: request.legalEntity.legalAddress.content.confidenceCriteria

    // The membership list is positional to the caller, so a rejection says which entry it is about.
    private fun toMembershipError(index: Int, error: GoldenRecordUpsertParseError): GoldenRecordUpsertParseError =
        when (error) {
            is SiteNotFound -> MembershipSiteNotFound(index, error.bpn)
            else -> error
        }

    private fun <T> ParseResult<T, GoldenRecordUpsertParseError>.recordIn(
        errors: MutableList<GoldenRecordUpsertParseError>
    ): T? =
        when (this) {
            is ParseResult.Success -> parsed
            is ParseResult.Failure -> { errors += this.errors; null }
        }

    private fun <T, E> single(
        results: List<ParseResult<T, E>>,
        errors: MutableList<GoldenRecordUpsertParseError>,
        toError: (E) -> GoldenRecordUpsertParseError
    ): T? =
        when (val result = results.single()) {
            is ParseResult.Success -> result.parsed
            is ParseResult.Failure -> { errors += result.errors.map(toError); null }
        }

}
