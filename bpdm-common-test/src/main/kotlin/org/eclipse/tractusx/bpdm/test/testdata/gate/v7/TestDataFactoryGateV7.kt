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

import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.input.BusinessPartnerInputV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.input.BusinessPartnerInputV7Request
import org.eclipse.tractusx.bpdm.gate.api.v7.model.businesspartner.output.AdditionalSiteOutputV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.changelog.ChangelogGateV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.relation.input.RelationPutV7Entry
import org.eclipse.tractusx.bpdm.pool.api.v7.model.address.LogisticAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.legalentity.LegalEntityWithLegalAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.pool.api.v7.model.site.SiteWithMainAddressVerboseV7Dto
import org.eclipse.tractusx.bpdm.orchestrator.api.model.BusinessPartnerRelations

/**
 * Top-level test data factory for the Gate V7 API.
 *
 * Hierarchy: [businessPartner] / [relation] → input / output → request / response
 *
 * Each leaf delegates to the corresponding fine-grained factory so seeding and
 * conversion logic stays in one place and is not duplicated.
 *
 * Usage examples:
 * ```
 * testDataFactory.businessPartner.input.request.fromSeed(seed)
 * testDataFactory.businessPartner.input.response.fromRequest(request)
 * testDataFactory.businessPartner.output.fromLegalEntity(input, legalEntity)
 * testDataFactory.relation.input.request.fromSeed(seed)
 * testDataFactory.relation.input.response.fromRequest(entry)
 * testDataFactory.relation.output.fromGoldenRecord(externalId, goldenRecord)
 * testDataFactory.changelog.ofOnePage(entry1, entry2)
 * ```
 */
class TestDataFactoryGateV7(
    private val bpInputRequestFactory: BusinessPartnerInputRequestV7Factory,
    private val bpInputDtoFactory: BusinessPartnerInputDtoV7Factory,
    private val bpOutputDtoFactory: BusinessPartnerOutputDtoV7Factory,
    private val relationInputRequestFactory: RelationInputRequestV7Factory,
    private val relationOutputDtoFactory: RelationOutputDtoV7Factory,
    private val pageChangeLogFactory: PageChangeLogV7Factory
) {

    val businessPartner = BusinessPartnerFactory()
    val relation = RelationFactory()
    val changelog = ChangelogFactory()

    inner class BusinessPartnerFactory {
        val input = InputFactory()
        val output = OutputFactory()

        inner class InputFactory {
            val request = RequestFactory()
            val response = ResponseFactory()

            inner class RequestFactory {
                fun fromSeed(seed: String): BusinessPartnerInputV7Request =
                    bpInputRequestFactory.fromSeed(seed)
            }

            inner class ResponseFactory {
                fun fromRequest(request: BusinessPartnerInputV7Request): BusinessPartnerInputV7Dto =
                    bpInputDtoFactory.fromRequest(request)
            }
        }

        inner class OutputFactory {
            fun fromLegalEntity(
                input: BusinessPartnerInputV7Dto,
                legalEntity: LegalEntityWithLegalAddressVerboseV7Dto
            ) = bpOutputDtoFactory.fromLegalEntity(input, legalEntity)

            fun fromLegalEntityOnSite(
                input: BusinessPartnerInputV7Dto,
                legalEntity: LegalEntityWithLegalAddressVerboseV7Dto,
                site: SiteWithMainAddressVerboseV7Dto
            ) = bpOutputDtoFactory.fromLegalEntityOnSite(input, legalEntity, site)

            fun fromSite(
                input: BusinessPartnerInputV7Dto,
                legalEntity: LegalEntityWithLegalAddressVerboseV7Dto,
                site: SiteWithMainAddressVerboseV7Dto
            ) = bpOutputDtoFactory.fromSite(input, legalEntity, site)

            fun fromAdditionalAddressOnSite(
                input: BusinessPartnerInputV7Dto,
                legalEntity: LegalEntityWithLegalAddressVerboseV7Dto,
                site: SiteWithMainAddressVerboseV7Dto,
                additionalAddress: LogisticAddressVerboseV7Dto,
                additionalSites: Collection<AdditionalSiteOutputV7Dto> = emptyList()
            ) = bpOutputDtoFactory.fromAdditionalAddressOnSite(input, legalEntity, site, additionalAddress, additionalSites)

            fun fromAdditionalAddressOnLegalEntity(
                input: BusinessPartnerInputV7Dto,
                legalEntity: LegalEntityWithLegalAddressVerboseV7Dto,
                additionalAddress: LogisticAddressVerboseV7Dto
            ) = bpOutputDtoFactory.fromAdditionalAddressOnLegalEntity(input, legalEntity, additionalAddress)
        }
    }

    inner class RelationFactory {
        val input = InputFactory()
        val output = OutputFactory()

        inner class InputFactory {
            val request = RequestFactory()
            val response = ResponseFactory()

            inner class RequestFactory {
                fun fromSeed(seed: String): RelationPutV7Entry =
                    relationInputRequestFactory.fromSeed(seed)
            }

            inner class ResponseFactory {
                fun fromRequest(entry: RelationPutV7Entry) =
                    relationInputRequestFactory.toExpectedResponse(entry)
            }
        }

        inner class OutputFactory {
            fun fromGoldenRecord(externalId: String, goldenRecordRelation: BusinessPartnerRelations) =
                relationOutputDtoFactory.fromGoldenRecord(externalId, goldenRecordRelation)

            fun fromInput(entry: RelationPutV7Entry) =
                relationOutputDtoFactory.fromInput(entry)
        }
    }

    inner class ChangelogFactory {
        fun ofOnePage(vararg entries: ChangelogGateV7Dto) =
            pageChangeLogFactory.ofOnePageWithoutInvalids(*entries)
    }
}
