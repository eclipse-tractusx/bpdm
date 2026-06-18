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

import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.RelationPutEntry
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.ChangelogGateDto
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityWithLegalAddressVerboseDto
import org.eclipse.tractusx.bpdm.pool.api.model.response.SiteWithMainAddressVerboseDto
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartnerRelations

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
                fun fromSeed(seed: String): BusinessPartnerInputRequest =
                    bpInputRequestFactory.fromSeed(seed)
            }

            inner class ResponseFactory {
                fun fromRequest(request: BusinessPartnerInputRequest): BusinessPartnerInputDto =
                    bpInputDtoFactory.fromRequest(request)
            }
        }

        inner class OutputFactory {
            fun fromLegalEntity(
                input: BusinessPartnerInputDto,
                legalEntity: LegalEntityWithLegalAddressVerboseDto
            ) = bpOutputDtoFactory.fromLegalEntity(input, legalEntity)

            fun fromLegalEntityOnSite(
                input: BusinessPartnerInputDto,
                legalEntity: LegalEntityWithLegalAddressVerboseDto,
                site: SiteWithMainAddressVerboseDto
            ) = bpOutputDtoFactory.fromLegalEntityOnSite(input, legalEntity, site)

            fun fromSite(
                input: BusinessPartnerInputDto,
                legalEntity: LegalEntityWithLegalAddressVerboseDto,
                site: SiteWithMainAddressVerboseDto
            ) = bpOutputDtoFactory.fromSite(input, legalEntity, site)

            fun fromAdditionalAddressOnSite(
                input: BusinessPartnerInputDto,
                legalEntity: LegalEntityWithLegalAddressVerboseDto,
                site: SiteWithMainAddressVerboseDto,
                additionalAddress: LogisticAddressVerboseDto
            ) = bpOutputDtoFactory.fromAdditionalAddressOnSite(input, legalEntity, site, additionalAddress)
        }
    }

    inner class RelationFactory {
        val input = InputFactory()
        val output = OutputFactory()

        inner class InputFactory {
            val request = RequestFactory()
            val response = ResponseFactory()

            inner class RequestFactory {
                fun fromSeed(seed: String): RelationPutEntry =
                    relationInputRequestFactory.fromSeed(seed)
            }

            inner class ResponseFactory {
                fun fromRequest(entry: RelationPutEntry) =
                    relationInputRequestFactory.toExpectedResponse(entry)
            }
        }

        inner class OutputFactory {
            fun fromGoldenRecord(externalId: String, goldenRecordRelation: BusinessPartnerRelations) =
                relationOutputDtoFactory.fromGoldenRecord(externalId, goldenRecordRelation)
        }
    }

    inner class ChangelogFactory {
        fun ofOnePage(vararg entries: ChangelogGateDto) =
            pageChangeLogFactory.ofOnePageWithoutInvalids(*entries)
    }
}
