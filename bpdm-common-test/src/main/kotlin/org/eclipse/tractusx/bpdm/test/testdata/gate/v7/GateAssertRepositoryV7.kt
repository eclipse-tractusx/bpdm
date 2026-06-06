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
import org.eclipse.tractusx.bpdm.gate.api.model.RelationDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.AddressComponentOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.AddressRepresentationInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.ChangelogGateDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.LegalEntityRepresentationInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.LegalEntityRepresentationOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.PageChangeLogDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.SharingStateDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.SiteRepresentationInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.SiteRepresentationOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.StatsSharingStatesResponse
import org.eclipse.tractusx.bpdm.test.util.InstantSecondsComparator
import org.eclipse.tractusx.bpdm.test.util.LocalDatetimeSecondsComparator
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
            BusinessPartnerOutputDto::createdAt.name,
            BusinessPartnerOutputDto::updatedAt.name
        )
        .withComparatorForType(instantSecondsComparator, Instant::class.java)
        .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
        .build()

    val outputComparisonConfigNoBpn: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder()
        .withIgnoredFields(
            BusinessPartnerOutputDto::createdAt.name,
            BusinessPartnerOutputDto::updatedAt.name,
            "legalEntity.${LegalEntityRepresentationOutputDto::legalEntityBpn.name}",
            "site.${SiteRepresentationOutputDto::siteBpn.name}",
            "address.${AddressComponentOutputDto::addressBpn.name}",
            BusinessPartnerOutputDto::nameParts.name
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
            BusinessPartnerOutputDto::createdAt.name,
            BusinessPartnerOutputDto::updatedAt.name,
            BusinessPartnerOutputDto::nameParts.name,
            BusinessPartnerOutputDto::identifiers.name,
            BusinessPartnerOutputDto::states.name,
            BusinessPartnerOutputDto::roles.name,
            BusinessPartnerOutputDto::isOwnCompanyData.name,
            BusinessPartnerOutputDto::externalSequenceTimestamp.name,
            BusinessPartnerOutputDto::scriptVariants.name,
            // legal entity: keep only legalName / shortName / legalForm
            "legalEntity.${LegalEntityRepresentationOutputDto::legalEntityBpn.name}",
            "legalEntity.${LegalEntityRepresentationOutputDto::confidenceCriteria.name}",
            "legalEntity.${LegalEntityRepresentationOutputDto::states.name}",
            "legalEntity.${LegalEntityRepresentationOutputDto::goldenRecordRelations.name}",
            // site: keep only name
            "site.${SiteRepresentationOutputDto::siteBpn.name}",
            "site.${SiteRepresentationOutputDto::confidenceCriteria.name}",
            "site.${SiteRepresentationOutputDto::states.name}",
            // address: keep only name / addressType / physical & alternative postal address
            "address.${AddressComponentOutputDto::addressBpn.name}",
            "address.${AddressComponentOutputDto::confidenceCriteria.name}",
            "address.${AddressComponentOutputDto::states.name}",
            "address.${AddressComponentOutputDto::identifiers.name}",
            "address.${AddressComponentOutputDto::goldenRecordRelations.name}"
        )
        .withComparatorForType(instantSecondsComparator, Instant::class.java)
        .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
        .build()

    /**
     * Compares ONLY the record's top-level [BusinessPartnerOutputDto.identifiers] and
     * [BusinessPartnerOutputDto.states]. These two fields surface the identifiers and states of the entity
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
            BusinessPartnerOutputDto::identifiers.name,
            BusinessPartnerOutputDto::states.name
        )
        .withComparatorForType(instantSecondsComparator, Instant::class.java)
        .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
        .build()

    fun assertBusinessPartnerInput(actual: Collection<BusinessPartnerInputDto>, expected: Collection<BusinessPartnerInputDto>) {
        Assertions.assertThat(actual.sortedBy { it.externalId }.map { it.sortContent() })
            .usingRecursiveComparison()
            .ignoringFields(
                BusinessPartnerInputDto::createdAt.name,
                BusinessPartnerInputDto::updatedAt.name
            )
            .isEqualTo(expected.sortedBy { it.externalId }.map { it.sortContent() })
    }

    fun assertBusinessPartnerInput(actual: PageDto<BusinessPartnerInputDto>, expected: PageDto<BusinessPartnerInputDto>) {
        assertPageHeader(actual, expected)
        assertBusinessPartnerInput(actual.content, expected.content)
    }

    fun assertBusinessPartnerOutput(
        actual: Collection<BusinessPartnerOutputDto>,
        expected: Collection<BusinessPartnerOutputDto>,
        config: RecursiveComparisonConfiguration = outputComparisonConfig
    ) {
        Assertions.assertThat(actual.sortedBy { it.externalId }.map { it.sortContent() })
            .usingRecursiveComparison(config)
            .isEqualTo(expected.sortedBy { it.externalId }.map { it.sortContent() })
    }

    fun assertBusinessPartnerOutput(
        actual: PageDto<BusinessPartnerOutputDto>,
        expected: PageDto<BusinessPartnerOutputDto>,
        config: RecursiveComparisonConfiguration = outputComparisonConfig
    ) {
        assertPageHeader(actual, expected)
        assertBusinessPartnerOutput(actual.content, expected.content, config)
    }

    fun assertSharingStates(actual: PageDto<SharingStateDto>, expected: PageDto<SharingStateDto>) {
        assertPageHeader(actual, expected)
        assertSharingStates(actual.content, expected.content)
    }

    fun assertSharingStates(actual: Collection<SharingStateDto>, expected: Collection<SharingStateDto>) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(
                SharingStateDto::updatedAt.name,
                SharingStateDto::sharingProcessStarted.name,
                SharingStateDto::taskId.name
            )
            .isEqualTo(expected)
    }

    fun assertSharingStateStats(actual: StatsSharingStatesResponse, expected: StatsSharingStatesResponse) {
        Assertions.assertThat(actual).isEqualTo(expected)
    }

    fun assertRelationSharingStates(actual: PageDto<RelationSharingStateDto>, expected: PageDto<RelationSharingStateDto>) {
        assertPageHeader(actual, expected)
        assertRelationSharingStates(actual.content, expected.content)
    }

    fun assertRelationSharingStates(actual: Collection<RelationSharingStateDto>, expected: Collection<RelationSharingStateDto>) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(
                RelationSharingStateDto::updatedAt.name,
                RelationSharingStateDto::sharingErrorMessage.name
            )
            .isEqualTo(expected)
    }

    fun assertRelationSharingStatePageMetadata(actual: PageDto<RelationSharingStateDto>, totalElements: Long, totalPages: Int, page: Int, contentSize: Int) {
        Assertions.assertThat(actual.totalElements).isEqualTo(totalElements)
        Assertions.assertThat(actual.totalPages).isEqualTo(totalPages)
        Assertions.assertThat(actual.page).isEqualTo(page)
        Assertions.assertThat(actual.contentSize).isEqualTo(contentSize)
    }

    fun assertRelation(actual: RelationDto, expected: RelationDto) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                RelationDto::createdAt.name,
                RelationDto::updatedAt.name
            )
            .isEqualTo(expected)
    }

    fun assertRelations(actual: Collection<RelationDto>, expected: Collection<RelationDto>) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                RelationDto::createdAt.name,
                RelationDto::updatedAt.name
            )
            .isEqualTo(expected)
    }

    fun assertRelations(actual: PageDto<RelationDto>, expected: PageDto<RelationDto>) {
        assertPageHeader(actual, expected)
        assertRelations(actual.content, expected.content)
    }

    fun assertRelationOutput(actual: Collection<RelationOutputDto>, expected: Collection<RelationOutputDto>) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(RelationOutputDto::updatedAt.name)
            .isEqualTo(expected)
    }

    fun assertRelationOutput(actual: PageDto<RelationOutputDto>, expected: PageDto<RelationOutputDto>) {
        assertPageHeader(actual, expected)
        assertRelationOutput(actual.content, expected.content)
    }

    fun assertRelationOutputInAnyDirection(actual: PageDto<RelationOutputDto>, expected: PageDto<RelationOutputDto>) {
        assertPageHeader(actual, expected)
        assertRelationOutputInAnyDirection(actual.content, expected.content)
    }

    fun assertRelationOutputInAnyDirection(actual: Collection<RelationOutputDto>, expected: Collection<RelationOutputDto>) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                RelationOutputDto::updatedAt.name,
                RelationOutputDto::sourceBpn.name,
                RelationOutputDto::targetBpn.name
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

    fun assertRelationPageMetadata(actual: PageDto<RelationDto>, totalElements: Long, totalPages: Int, page: Int, contentSize: Int) {
        Assertions.assertThat(actual.totalElements).isEqualTo(totalElements)
        Assertions.assertThat(actual.totalPages).isEqualTo(totalPages)
        Assertions.assertThat(actual.page).isEqualTo(page)
        Assertions.assertThat(actual.contentSize).isEqualTo(contentSize)
    }

    fun assertChangelog(actual: PageChangeLogDto<ChangelogGateDto>, expected: PageChangeLogDto<ChangelogGateDto>) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(PageChangeLogDto<*>::content.name)
            .isEqualTo(expected)

        Assertions.assertThat(actual.content)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(ChangelogGateDto::timestamp.name)
            .isEqualTo(expected.content)
    }

    private fun assertPageHeader(actual: PageDto<*>, expected: PageDto<*>) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(PageDto<*>::content.name)
            .isEqualTo(expected)
    }

    private fun BusinessPartnerInputDto.sortContent() =
        copy(
            identifiers = identifiers.sortedBy { it.value },
            states = states.sortedBy { it.validFrom?.toString() },
            roles = roles.sortedBy { it.name },
            scriptVariants = scriptVariants.sortedBy { it.scriptCode },
            legalEntity = legalEntity.sortContent(),
            site = site.sortContent(),
            address = address.sortContent()
        )

    private fun LegalEntityRepresentationInputDto.sortContent() =
        copy(states = states.sortedBy { it.validFrom?.toString() })

    private fun SiteRepresentationInputDto.sortContent() =
        copy(states = states.sortedBy { it.validFrom?.toString() })

    private fun AddressRepresentationInputDto.sortContent() =
        copy(states = states.sortedBy { it.validFrom?.toString() })

    private fun BusinessPartnerOutputDto.sortContent() =
        copy(
            identifiers = identifiers.sortedBy { it.value },
            states = states.sortedBy { it.validFrom?.toString() },
            roles = roles.sortedBy { it.name },
            scriptVariants = scriptVariants.sortedBy { it.scriptCode },
            legalEntity = legalEntity.sortContent(),
            site = site?.sortContent(),
            address = address.sortContent()
        )

    private fun LegalEntityRepresentationOutputDto.sortContent() =
        copy(
            states = states.sortedBy { it.validFrom?.toString() },
            goldenRecordRelations = goldenRecordRelations.sortedBy { it.sourceBpn }
        )

    private fun SiteRepresentationOutputDto.sortContent() =
        copy(states = states.sortedBy { it.validFrom?.toString() })

    private fun AddressComponentOutputDto.sortContent() =
        copy(
            states = states.sortedBy { it.validFrom?.toString() },
            identifiers = identifiers.sortedBy { it.value },
            goldenRecordRelations = goldenRecordRelations.sortedBy { it.sourceBpn }
        )
}