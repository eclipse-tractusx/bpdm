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

package org.eclipse.tractusx.bpdm.test.testdata.gate.v7

import org.assertj.core.api.Assertions
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.input.AddressRepresentationInputV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.input.BusinessPartnerInputV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.input.LegalEntityRepresentationInputV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.input.SiteRepresentationInputV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.output.AddressComponentOutputV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.output.BusinessPartnerOutputV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.output.LegalEntityRepresentationOutputV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.output.SiteRepresentationOutputV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.sharingstate.SharingStateV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.changelog.ChangelogGateV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.changelog.PageChangeLogV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.error.GateErrorV7Response
import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.input.RelationV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.output.RelationOutputV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.sharingstate.RelationSharingStateV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.stats.StatsSharingStatesV7Response
import org.eclipse.tractusx.bpdm.test.util.InstantSecondsComparator
import org.eclipse.tractusx.bpdm.test.util.LocalDatetimeSecondsComparator
import org.eclipse.tractusx.bpdm.orchestrator.api.model.SharingMemberRecordUpdateRequest
import java.time.Instant
import java.time.LocalDateTime

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
class GateAssertRepositoryV7(
    private val instantSecondsComparator: InstantSecondsComparator,
    private val localDatetimeSecondsComparator: LocalDatetimeSecondsComparator
) {

    val outputComparisonConfig: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder()
        .withIgnoredFields(
            BusinessPartnerOutputV7Dto::createdAt.name,
            BusinessPartnerOutputV7Dto::updatedAt.name
        )
        .withComparatorForType(instantSecondsComparator, Instant::class.java)
        .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
        .build()

    val outputComparisonConfigNoBpn: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder()
        .withIgnoredFields(
            BusinessPartnerOutputV7Dto::createdAt.name,
            BusinessPartnerOutputV7Dto::updatedAt.name,
            "legalEntity.${LegalEntityRepresentationOutputV7Dto::legalEntityBpn.name}",
            "site.${SiteRepresentationOutputV7Dto::siteBpn.name}",
            "address.${AddressComponentOutputV7Dto::addressBpn.name}",
            "legalEntity.${LegalEntityRepresentationOutputV7Dto::updatedAt.name}",
            "site.${SiteRepresentationOutputV7Dto::updatedAt.name}",
            "address.${AddressComponentOutputV7Dto::updatedAt.name}",
            BusinessPartnerOutputV7Dto::nameParts.name
        )
        .withComparatorForType(instantSecondsComparator, Instant::class.java)
        .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
        .build()

    /**
     * Compares only the master data of an output business partner: the descriptive legal entity, site
     * and address attributes (legal name / short name / legal form, site name, address name / type /
     * postal addresses).
     *
     * Everything else is ignored on purpose: identifiers, states, roles, confidence criteria, golden
     * record relations, BPNs, script variants and timestamps. Those are produced by the test data
     * factories with a lot of derived logic and are covered by dedicated tests, so ignoring them here
     * keeps master-data assertions stable when that factory logic is refactored.
     */
    val outputMasterDataComparisonConfig: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder()
        .withIgnoredFields(
            BusinessPartnerOutputV7Dto::createdAt.name,
            BusinessPartnerOutputV7Dto::updatedAt.name,
            BusinessPartnerOutputV7Dto::nameParts.name,
            BusinessPartnerOutputV7Dto::identifiers.name,
            BusinessPartnerOutputV7Dto::states.name,
            BusinessPartnerOutputV7Dto::roles.name,
            BusinessPartnerOutputV7Dto::isOwnCompanyData.name,
            BusinessPartnerOutputV7Dto::externalSequenceTimestamp.name,
            BusinessPartnerOutputV7Dto::scriptVariants.name,
            // legal entity: keep only legalName / shortName / legalForm
            "legalEntity.${LegalEntityRepresentationOutputV7Dto::legalEntityBpn.name}",
            "legalEntity.${LegalEntityRepresentationOutputV7Dto::confidenceCriteria.name}",
            "legalEntity.${LegalEntityRepresentationOutputV7Dto::states.name}",
            "legalEntity.${LegalEntityRepresentationOutputV7Dto::goldenRecordRelations.name}",
            "legalEntity.${LegalEntityRepresentationOutputV7Dto::updatedAt.name}",
            // site: keep only name
            "site.${SiteRepresentationOutputV7Dto::siteBpn.name}",
            "site.${SiteRepresentationOutputV7Dto::confidenceCriteria.name}",
            "site.${SiteRepresentationOutputV7Dto::states.name}",
            "site.${SiteRepresentationOutputV7Dto::goldenRecordRelations.name}",
            "site.${SiteRepresentationOutputV7Dto::updatedAt.name}",
            // address: keep only name / addressType / physical & alternative postal address
            "address.${AddressComponentOutputV7Dto::addressBpn.name}",
            "address.${AddressComponentOutputV7Dto::confidenceCriteria.name}",
            "address.${AddressComponentOutputV7Dto::states.name}",
            "address.${AddressComponentOutputV7Dto::identifiers.name}",
            "address.${AddressComponentOutputV7Dto::goldenRecordRelations.name}",
            "address.${AddressComponentOutputV7Dto::updatedAt.name}"
        )
        .withComparatorForType(instantSecondsComparator, Instant::class.java)
        .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
        .build()

    /**
     * Compares ONLY the record's top-level [BusinessPartnerOutputV7Dto.identifiers] and
     * [BusinessPartnerOutputV7Dto.states]. These two fields surface the identifiers and states of the entity
     * the record reflects, and which entity that is depends on the record type:
     *  - legal entity record   -> the legal entity's identifiers and states
     *  - site record           -> NO identifiers, the site's states
     *  - additional address    -> the address's identifiers and states
     *
     * Everything else is ignored on purpose (the inverse of [outputMasterDataComparisonConfig]): descriptive
     * master data, BPNs, the nested per-entity identifiers/states, roles, confidence criteria, relations,
     * script variants and timestamps are covered by their own tests. Restricting the comparison to these two
     * fields keeps the surfacing rule the single subject of these assertions.
     */
    val outputTopLevelIdentifiersAndStatesComparisonConfig: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder()
        .withComparedFields(
            BusinessPartnerOutputV7Dto::identifiers.name,
            BusinessPartnerOutputV7Dto::states.name
        )
        .withComparatorForType(instantSecondsComparator, Instant::class.java)
        .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
        .build()

    /**
     * Compares ONLY the record's [BusinessPartnerOutputV7Dto.scriptVariants]. This field reflects the merged
     * script variants of the golden records the record was refined to (the matched entity and its parents),
     * keyed by script code. Everything else - master data, identifiers, states, BPNs, confidence criteria,
     * relations and timestamps - is ignored on purpose and covered by its own tests, so that script-variant
     * assertions stay the single subject here. [sortContent] already sorts the variants by script code, so
     * ordering does not affect the comparison.
     */
    val outputScriptVariantsComparisonConfig: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder()
        .withComparedFields(
            BusinessPartnerOutputV7Dto::scriptVariants.name
        )
        .withComparatorForType(instantSecondsComparator, Instant::class.java)
        .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
        .build()

    /**
     * Compares ONLY the record's [BusinessPartnerOutputV7Dto.additionalSites]: the further sites the
     * record's address belongs to beyond the site the record itself follows, each surfaced as a
     * BPNS/name pair. Everything else - master data, identifiers, states, BPNs, confidence criteria,
     * relations, script variants and timestamps - is ignored on purpose and covered by its own tests,
     * so additional-site assertions stay the single subject here. [sortContent] already sorts the
     * additional sites by BPNS, so ordering does not affect the comparison.
     */
    val outputAdditionalSitesComparisonConfig: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder()
        .withComparedFields(
            BusinessPartnerOutputV7Dto::additionalSites.name
        )
        .withComparatorForType(instantSecondsComparator, Instant::class.java)
        .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
        .build()

    fun assertBusinessPartnerInput(actual: Collection<BusinessPartnerInputV7Dto>, expected: Collection<BusinessPartnerInputV7Dto>) {
        Assertions.assertThat(actual.sortedBy { it.externalId }.map { it.sortContent() })
            .usingRecursiveComparison()
            .ignoringFields(
                BusinessPartnerInputV7Dto::createdAt.name,
                BusinessPartnerInputV7Dto::updatedAt.name
            )
            .isEqualTo(expected.sortedBy { it.externalId }.map { it.sortContent() })
    }

    fun assertBusinessPartnerInput(actual: PageDto<BusinessPartnerInputV7Dto>, expected: PageDto<BusinessPartnerInputV7Dto>) {
        assertPageHeader(actual, expected)
        assertBusinessPartnerInput(actual.content, expected.content)
    }

    fun assertBusinessPartnerInputRead(actual: PageDto<BusinessPartnerInputV7Dto>, expected: PageDto<BusinessPartnerInputV7Dto>) {
        assertBusinessPartnerInput(actual, expected)
        val expectedById = expected.content.associateBy { it.externalId }
        actual.content.forEach {
            val previous = expectedById.getValue(it.externalId)
            assertTimestampsUnchanged(it.createdAt, it.updatedAt, previous.createdAt, previous.updatedAt)
        }
    }

    fun assertTimestampsUnchanged(createdAt: Instant, updatedAt: Instant, expectedCreatedAt: Instant, expectedUpdatedAt: Instant) {
        Assertions.assertThat(createdAt).isEqualTo(expectedCreatedAt)
        Assertions.assertThat(updatedAt).isEqualTo(expectedUpdatedAt)
        Assertions.assertThat(createdAt.nano % 1000).isZero()
        Assertions.assertThat(updatedAt.nano % 1000).isZero()
    }

    fun assertTimestampsUpdated(createdAt: Instant, updatedAt: Instant, previousCreatedAt: Instant, previousUpdatedAt: Instant) {
        Assertions.assertThat(createdAt).isEqualTo(previousCreatedAt)
        Assertions.assertThat(updatedAt).isAfter(previousUpdatedAt)
    }

    fun assertSharingStateTimestamp(actual: SharingStateV7Dto, expected: Instant) {
        Assertions.assertThat(actual.updatedAt).isEqualTo(expected)
        Assertions.assertThat(actual.updatedAt.nano % 1000).isZero()
    }

    fun assertBusinessPartnerOutput(
        actual: Collection<BusinessPartnerOutputV7Dto>,
        expected: Collection<BusinessPartnerOutputV7Dto>,
        config: RecursiveComparisonConfiguration = outputComparisonConfig
    ) {
        Assertions.assertThat(actual.sortedBy { it.externalId }.map { it.sortContent() })
            .usingRecursiveComparison(config)
            .isEqualTo(expected.sortedBy { it.externalId }.map { it.sortContent() })
    }

    fun assertBusinessPartnerOutput(
        actual: PageDto<BusinessPartnerOutputV7Dto>,
        expected: PageDto<BusinessPartnerOutputV7Dto>,
        config: RecursiveComparisonConfiguration = outputComparisonConfig
    ) {
        assertPageHeader(actual, expected)
        assertBusinessPartnerOutput(actual.content, expected.content, config)
    }

    fun assertSharingStates(actual: PageDto<SharingStateV7Dto>, expected: PageDto<SharingStateV7Dto>) {
        assertPageHeader(actual, expected)
        assertSharingStates(actual.content, expected.content)
    }

    fun assertSharingStates(actual: Collection<SharingStateV7Dto>, expected: Collection<SharingStateV7Dto>) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(
                SharingStateV7Dto::updatedAt.name,
                SharingStateV7Dto::sharingProcessStarted.name,
                SharingStateV7Dto::taskId.name
            )
            .isEqualTo(expected)
    }

    fun assertSharingMemberRecordUpdates(actual: Collection<SharingMemberRecordUpdateRequest>, expected: Collection<SharingMemberRecordUpdateRequest>) {
        Assertions.assertThat(actual.sortedBy { it.recordId })
            .usingRecursiveComparison()
            .isEqualTo(expected.sortedBy { it.recordId })
    }

    fun assertErrorPerExternalId(actual: GateErrorV7Response, externalIds: Collection<String>) {
        Assertions.assertThat(actual.error).hasSameSizeAs(externalIds)
        externalIds.forEach { externalId ->
            Assertions.assertThat(actual.error).anySatisfy { Assertions.assertThat(it).contains("'$externalId'") }
        }
    }

    fun assertSharingStateStats(actual: StatsSharingStatesV7Response, expected: StatsSharingStatesV7Response) {
        Assertions.assertThat(actual).isEqualTo(expected)
    }

    fun assertRelationSharingStates(actual: PageDto<RelationSharingStateV7Dto>, expected: PageDto<RelationSharingStateV7Dto>) {
        assertPageHeader(actual, expected)
        assertRelationSharingStates(actual.content, expected.content)
    }

    fun assertRelationSharingStates(actual: Collection<RelationSharingStateV7Dto>, expected: Collection<RelationSharingStateV7Dto>) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(
                RelationSharingStateV7Dto::updatedAt.name,
                RelationSharingStateV7Dto::sharingErrorMessage.name
            )
            .isEqualTo(expected)
    }

    fun assertRelationSharingStatePageMetadata(actual: PageDto<RelationSharingStateV7Dto>, totalElements: Long, totalPages: Int, page: Int, contentSize: Int) {
        Assertions.assertThat(actual.totalElements).isEqualTo(totalElements)
        Assertions.assertThat(actual.totalPages).isEqualTo(totalPages)
        Assertions.assertThat(actual.page).isEqualTo(page)
        Assertions.assertThat(actual.contentSize).isEqualTo(contentSize)
    }

    fun assertRelation(actual: RelationV7Dto, expected: RelationV7Dto) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                RelationV7Dto::createdAt.name,
                RelationV7Dto::updatedAt.name
            )
            .isEqualTo(expected)
    }

    fun assertRelations(actual: Collection<RelationV7Dto>, expected: Collection<RelationV7Dto>) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                RelationV7Dto::createdAt.name,
                RelationV7Dto::updatedAt.name
            )
            .isEqualTo(expected)
    }

    fun assertRelations(actual: PageDto<RelationV7Dto>, expected: PageDto<RelationV7Dto>) {
        assertPageHeader(actual, expected)
        assertRelations(actual.content, expected.content)
        val expectedById = expected.content.associateBy { it.externalId }
        actual.content.forEach {
            val previous = expectedById.getValue(it.externalId)
            assertTimestampsUnchanged(it.createdAt, it.updatedAt, previous.createdAt, previous.updatedAt)
        }
    }

    fun assertRelationOutput(actual: Collection<RelationOutputV7Dto>, expected: Collection<RelationOutputV7Dto>) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(RelationOutputV7Dto::updatedAt.name)
            .isEqualTo(expected)
    }

    fun assertRelationOutput(actual: PageDto<RelationOutputV7Dto>, expected: PageDto<RelationOutputV7Dto>) {
        assertPageHeader(actual, expected)
        assertRelationOutput(actual.content, expected.content)
    }

    fun assertRelationOutputInAnyDirection(actual: PageDto<RelationOutputV7Dto>, expected: PageDto<RelationOutputV7Dto>) {
        assertPageHeader(actual, expected)
        assertRelationOutputInAnyDirection(actual.content, expected.content)
    }

    fun assertRelationOutputInAnyDirection(actual: Collection<RelationOutputV7Dto>, expected: Collection<RelationOutputV7Dto>) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                RelationOutputV7Dto::updatedAt.name,
                RelationOutputV7Dto::sourceBpn.name,
                RelationOutputV7Dto::targetBpn.name
            )
            .isEqualTo(expected)

        actual.forEach { actualDto ->
            val expectedDto = expected.single { it.externalId == actualDto.externalId }
            val actualBpns = setOf(actualDto.sourceBpn, actualDto.targetBpn)
            val expectedBpns = setOf(expectedDto.sourceBpn, expectedDto.targetBpn)
            Assertions.assertThat(actualBpns)
                .describedAs("BPN pair for relation '${actualDto.externalId}' must match in any direction")
                .isEqualTo(expectedBpns)
        }
    }

    fun assertRelationPageMetadata(actual: PageDto<RelationV7Dto>, totalElements: Long, totalPages: Int, page: Int, contentSize: Int) {
        Assertions.assertThat(actual.totalElements).isEqualTo(totalElements)
        Assertions.assertThat(actual.totalPages).isEqualTo(totalPages)
        Assertions.assertThat(actual.page).isEqualTo(page)
        Assertions.assertThat(actual.contentSize).isEqualTo(contentSize)
    }

    fun assertChangelog(actual: PageChangeLogV7Dto<ChangelogGateV7Dto>, expected: PageChangeLogV7Dto<ChangelogGateV7Dto>) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(PageChangeLogV7Dto<*>::content.name)
            .isEqualTo(expected)

        Assertions.assertThat(actual.content)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(ChangelogGateV7Dto::timestamp.name)
            .isEqualTo(expected.content)
    }

    private fun assertPageHeader(actual: PageDto<*>, expected: PageDto<*>) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(PageDto<*>::content.name)
            .isEqualTo(expected)
    }

    private fun BusinessPartnerInputV7Dto.sortContent() =
        copy(
            identifiers = identifiers.sortedBy { it.value },
            states = states.sortedBy { it.validFrom?.toString() },
            roles = roles.sortedBy { it.name },
            scriptVariants = scriptVariants.sortedBy { it.scriptCode },
            additionalSites = additionalSites.sortedBy { it.siteBpn ?: it.name },
            legalEntity = legalEntity.sortContent(),
            site = site.sortContent(),
            address = address.sortContent()
        )

    private fun LegalEntityRepresentationInputV7Dto.sortContent() =
        copy(states = states.sortedBy { it.validFrom?.toString() })

    private fun SiteRepresentationInputV7Dto.sortContent() =
        copy(states = states.sortedBy { it.validFrom?.toString() })

    private fun AddressRepresentationInputV7Dto.sortContent() =
        copy(states = states.sortedBy { it.validFrom?.toString() })

    private fun BusinessPartnerOutputV7Dto.sortContent() =
        copy(
            identifiers = identifiers.sortedBy { it.value },
            states = states.sortedBy { it.validFrom?.toString() },
            roles = roles.sortedBy { it.name },
            scriptVariants = scriptVariants.sortedBy { it.scriptCode },
            additionalSites = additionalSites.sortedBy { it.siteBpn },
            legalEntity = legalEntity.sortContent(),
            site = site?.sortContent(),
            address = address.sortContent()
        )

    private fun LegalEntityRepresentationOutputV7Dto.sortContent() =
        copy(
            states = states.sortedBy { it.validFrom?.toString() },
            goldenRecordRelations = goldenRecordRelations.sortedBy { it.sourceBpn }
        )

    private fun SiteRepresentationOutputV7Dto.sortContent() =
        copy(
            states = states.sortedBy { it.validFrom?.toString() },
            goldenRecordRelations = goldenRecordRelations.sortedBy { it.sourceBpn }
        )

    private fun AddressComponentOutputV7Dto.sortContent() =
        copy(
            states = states.sortedBy { it.validFrom?.toString() },
            identifiers = identifiers.sortedBy { it.value },
            goldenRecordRelations = goldenRecordRelations.sortedBy { it.sourceBpn }
        )
}