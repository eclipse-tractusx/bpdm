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

package org.eclipse.tractusx.bpdm.pool.v6.util

import org.assertj.core.api.Assertions
import org.assertj.core.api.ObjectAssert
import org.assertj.core.api.RecursiveComparisonAssert
import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressPartnerCreateV6ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressPartnerCreateVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.AddressPartnerUpdateV6ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.address.LogisticAddressVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.error.ErrorInfoV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerCreateV6ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerCreateVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityPartnerUpdateV6ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity.LegalEntityWithLegalAddressVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.CountryV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.FieldQualityRuleV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.metadata.QualityLevelV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SitePartnerCreateV6ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SitePartnerCreateVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SitePartnerUpdateV6ResponseWrapper
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteVerboseV6Dto
import org.eclipse.tractusx.bpdm.pool.api.v6.model.site.SiteWithMainAddressVerboseV6Dto
import org.eclipse.tractusx.bpdm.test.util.LocalDatetimeSecondsComparator
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.LocalDateTime

/**
 * Offers util functionalities for comparing complex API V6 objects under test
 */
class AssertRepositoryV6(
    private val localDatetimeSecondsComparator: LocalDatetimeSecondsComparator
) {
    fun assertCountryPage(actual: PageDto<CountryV6Dto>, page: Int, pageSize: Int) {
        Assertions.assertThat(actual.page).isEqualTo(page)
        Assertions.assertThat(actual.totalElements).isGreaterThan((page * pageSize).toLong())
        Assertions.assertThat(actual.totalPages).isEqualTo(((actual.totalElements + pageSize - 1) / pageSize).toInt())
        val expectedSize = minOf(pageSize.toLong(), actual.totalElements - page.toLong() * pageSize).toInt()
        Assertions.assertThat(actual.contentSize).isEqualTo(expectedSize)
        Assertions.assertThat(actual.content).hasSize(expectedSize)
        Assertions.assertThat(actual.content.map { it.countryCode }).isSorted()
    }

    fun assertCountryCatalog(actual: Collection<CountryV6Dto>, expectedCountry: CountryV6Dto) {
        Assertions.assertThat(actual).contains(expectedCountry)
        Assertions.assertThat(actual).allSatisfy {
            Assertions.assertThat(it.countryCode).isNotBlank()
            Assertions.assertThat(it.name).isNotBlank()
        }
    }

    fun assertFieldQualityRules(actual: Collection<FieldQualityRuleV6Dto>, country: String) {
        Assertions.assertThat(actual).isNotEmpty()
        Assertions.assertThat(actual).allSatisfy {
            Assertions.assertThat(it.country).isEqualTo(country)
            Assertions.assertThat(it.qualityLevel).isNotEqualTo(QualityLevelV6.FORBIDDEN)
        }
    }

    fun assertNotFound(actual: WebClientResponseException.NotFound, expectedDetail: String) {
        Assertions.assertThat(actual.responseBodyAsString).contains(expectedDetail)
    }
    companion object{
        private object LegalEntityUpsertResponsePaths{
            val entities: String = LegalEntityPartnerCreateV6ResponseWrapper::entities.name
            val errors: String = LegalEntityPartnerCreateV6ResponseWrapper::errors.name
            val legalEntity: String = "$entities.${LegalEntityPartnerCreateVerboseV6Dto::legalEntity.name}"
            val legalAddress: String = "$entities.${LegalEntityPartnerCreateVerboseV6Dto::legalAddress.name}"
            val legalEntityCreatedAt: String = "${legalEntity}.${LegalEntityVerboseV6Dto::createdAt.name}"
            val legalEntityUpdatedAt: String = "${legalEntity}.${LegalEntityVerboseV6Dto::updatedAt.name}"
            val legalAddressCreatedAt: String = "${legalAddress}.${LegalEntityVerboseV6Dto::createdAt.name}"
            val legalAddressUpdatedAt: String = "${legalAddress}.${LegalEntityVerboseV6Dto::updatedAt.name}"
            val bpnL: String = "${legalEntity}.${LegalEntityVerboseV6Dto::bpnl.name}"
            val currentness: String = "${legalEntity}.${LegalEntityVerboseV6Dto::currentness.name}"
            val bpnA: String = "${legalAddress}.${LogisticAddressVerboseV6Dto::bpna.name}"
            val bpnLegalEntity: String = "${legalAddress}.${LogisticAddressVerboseV6Dto::bpnLegalEntity.name}"
            val errorMessage: String = "${errors}.${ErrorInfoV6<*>::message.name}"
        }

        private object LegalEntityWithLegalAddressVerboseDtoPaths{
            val content = PageDto<*>::content.name
            val legalEntity = "${content}.${LegalEntityWithLegalAddressVerboseV6Dto::legalEntity.name}"
            val legalAddress =  "${content}.${LegalEntityWithLegalAddressVerboseV6Dto::legalAddress.name}"
            val legalEntityCreatedAt =  "${legalEntity}.${LegalEntityVerboseV6Dto::createdAt.name}"
            val legalEntityUpdatedAt =  "${legalEntity}.${LegalEntityVerboseV6Dto::updatedAt.name}"
            val legalAddressCreatedAt =  "${legalAddress}.${LogisticAddressVerboseV6Dto::createdAt.name}"
            val legalAddressUpdatedAt =  "${legalAddress}.${LogisticAddressVerboseV6Dto::updatedAt.name}"
        }

        private object LegalEntityGetPaths{
            private val legalEntity = LegalEntityWithLegalAddressVerboseV6Dto::legalEntity.name
            private val  legalAddress = LegalEntityWithLegalAddressVerboseV6Dto::legalAddress.name
            val legalEntityCreatedAt =  "${legalEntity}.${LegalEntityVerboseV6Dto::createdAt.name}"
            val legalEntityUpdatedAt =  "${legalEntity}.${LegalEntityVerboseV6Dto::updatedAt.name}"
            val legalAddressCreatedAt =  "${legalAddress}.${LogisticAddressVerboseV6Dto::createdAt.name}"
            val legalAddressUpdatedAt =  "${legalAddress}.${LogisticAddressVerboseV6Dto::updatedAt.name}"
        }

        private object SiteUpsertResponsePaths{
            val entities: String = SitePartnerCreateV6ResponseWrapper::entities.name
            val errors: String = SitePartnerCreateV6ResponseWrapper::errors.name
            val site: String = "$entities.${SitePartnerCreateVerboseV6Dto::site.name}"
            val mainAddress: String = "$entities.${SitePartnerCreateVerboseV6Dto::mainAddress.name}"
            val siteCreatedAt: String = "${site}.${SiteVerboseV6Dto::createdAt.name}"
            val siteUpdatedAt: String = "${site}.${SiteVerboseV6Dto::updatedAt.name}"
            val mainAddressCreatedAt: String = "${mainAddress}.${SiteVerboseV6Dto::createdAt.name}"
            val mainAddressUpdatedAt: String = "${mainAddress}.${SiteVerboseV6Dto::updatedAt.name}"
            val bpnS: String = "${site}.${SiteVerboseV6Dto::bpns.name}"
            val bpnA: String = "${mainAddress}.${LogisticAddressVerboseV6Dto::bpna.name}"
            val bpnSite: String = "${mainAddress}.${LogisticAddressVerboseV6Dto::bpnSite.name}"
            val errorMessage: String = "${errors}.${ErrorInfoV6<*>::message.name}"
        }

        private object SiteSearchResponsePaths{
            val content = PageDto<*>::content.name
            val site = "${content}.${SiteWithMainAddressVerboseV6Dto::site.name}"
            val mainAddress =  "${content}.${SiteWithMainAddressVerboseV6Dto::mainAddress.name}"
            val siteCreatedAt =  "${site}.${SiteVerboseV6Dto::createdAt.name}"
            val siteUpdatedAt =  "${site}.${SiteVerboseV6Dto::updatedAt.name}"
            val mainAddressCreatedAt =  "${mainAddress}.${LogisticAddressVerboseV6Dto::createdAt.name}"
            val mainAddressUpdatedAt =  "${mainAddress}.${LogisticAddressVerboseV6Dto::updatedAt.name}"
        }

        private object SiteGetResponsePaths{
            private val site = SiteWithMainAddressVerboseV6Dto::site.name
            private val mainAddress =  SiteWithMainAddressVerboseV6Dto::mainAddress.name
            val siteCreatedAt =  "${site}.${SiteVerboseV6Dto::createdAt.name}"
            val siteUpdatedAt =  "${site}.${SiteVerboseV6Dto::updatedAt.name}"
            val mainAddressCreatedAt =  "${mainAddress}.${LogisticAddressVerboseV6Dto::createdAt.name}"
            val mainAddressUpdatedAt =  "${mainAddress}.${LogisticAddressVerboseV6Dto::updatedAt.name}"
        }

        private object AdditionalAddressUpsertPaths{
            private val entities = AddressPartnerCreateV6ResponseWrapper::entities.name
            private val errors =  AddressPartnerCreateV6ResponseWrapper::errors.name
            private val address =  "${entities}.${AddressPartnerCreateVerboseV6Dto::address.name}"
            val bpnA = "${address}.${LogisticAddressVerboseV6Dto::bpna.name}"
            val createdAt = "${address}.${LogisticAddressVerboseV6Dto::createdAt.name}"
            val updatedAt = "${address}.${LogisticAddressVerboseV6Dto::updatedAt.name}"
            val errorMessage = "${errors}.${ErrorInfoV6<*>::message.name}"
        }

        private object AddressUpdatePaths{
            private val entities = AddressPartnerUpdateV6ResponseWrapper::entities.name
            private val errors =  AddressPartnerUpdateV6ResponseWrapper::errors.name
            val createdAt = "${entities}.${LogisticAddressVerboseV6Dto::createdAt.name}"
            val updatedAt = "${entities}.${LogisticAddressVerboseV6Dto::updatedAt.name}"
            val errorMessage = "${errors}.${ErrorInfoV6<*>::message.name}"
        }

    }


    fun assertLegalEntityCreate(actual: LegalEntityPartnerCreateV6ResponseWrapper, expected: LegalEntityPartnerCreateV6ResponseWrapper){
        Assertions.assertThat(actual)
            .usingRecursiveLegalEntityUpsertComparison()
            .isEqualTo(expected)
    }

    fun assertLegalEntityUpdate(actual: LegalEntityPartnerUpdateV6ResponseWrapper, expected: LegalEntityPartnerUpdateV6ResponseWrapper){
        Assertions.assertThat(actual)
            .usingRecursiveLegalEntityUpsertComparison()
            .isEqualTo(expected)
    }

    fun assertLegalEntitySearch(actual: PageDto<LegalEntityWithLegalAddressVerboseV6Dto>, expected: PageDto<LegalEntityWithLegalAddressVerboseV6Dto>) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                LegalEntityWithLegalAddressVerboseDtoPaths.legalEntityCreatedAt,
                LegalEntityWithLegalAddressVerboseDtoPaths.legalEntityUpdatedAt,
                LegalEntityWithLegalAddressVerboseDtoPaths.legalAddressCreatedAt,
                LegalEntityWithLegalAddressVerboseDtoPaths.legalAddressUpdatedAt
            )
            .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
            .isEqualTo(expected)
    }

    fun assertLegalEntityGet(actual: LegalEntityWithLegalAddressVerboseV6Dto, expected: LegalEntityWithLegalAddressVerboseV6Dto) {
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                LegalEntityGetPaths.legalEntityCreatedAt,
                LegalEntityGetPaths.legalEntityUpdatedAt,
                LegalEntityGetPaths.legalAddressCreatedAt,
                LegalEntityGetPaths.legalAddressUpdatedAt
            )
            .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
            .isEqualTo(expected)
    }

    fun assertSiteCreate(actual: SitePartnerCreateV6ResponseWrapper, expected: SitePartnerCreateV6ResponseWrapper){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                SiteUpsertResponsePaths.siteCreatedAt,
                SiteUpsertResponsePaths.siteUpdatedAt,
                SiteUpsertResponsePaths.mainAddressCreatedAt,
                SiteUpsertResponsePaths.mainAddressUpdatedAt,
                SiteUpsertResponsePaths.bpnS,
                SiteUpsertResponsePaths.bpnA,
                SiteUpsertResponsePaths.bpnSite,
                SiteUpsertResponsePaths.errorMessage
            )
            .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
            .isEqualTo(expected)
    }

    fun assertSiteUpdate(actual: SitePartnerUpdateV6ResponseWrapper, expected: SitePartnerUpdateV6ResponseWrapper){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                SiteUpsertResponsePaths.siteCreatedAt,
                SiteUpsertResponsePaths.siteUpdatedAt,
                SiteUpsertResponsePaths.mainAddressCreatedAt,
                SiteUpsertResponsePaths.mainAddressUpdatedAt,
                SiteUpsertResponsePaths.bpnA,
                SiteUpsertResponsePaths.errorMessage
            )
            .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
            .isEqualTo(expected)
    }

    fun assertLegalAddressSiteCreate(actual: SitePartnerCreateV6ResponseWrapper, expected: SitePartnerCreateV6ResponseWrapper){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                SiteUpsertResponsePaths.siteCreatedAt,
                SiteUpsertResponsePaths.siteUpdatedAt,
                SiteUpsertResponsePaths.mainAddressCreatedAt,
                SiteUpsertResponsePaths.mainAddressUpdatedAt,
                SiteUpsertResponsePaths.bpnS,
                SiteUpsertResponsePaths.bpnSite,
                SiteUpsertResponsePaths.errorMessage
            )
            .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
            .isEqualTo(expected)
    }

    fun assertSiteSearch(actual: PageDto<SiteWithMainAddressVerboseV6Dto>, expected: PageDto<SiteWithMainAddressVerboseV6Dto>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                SiteSearchResponsePaths.siteCreatedAt,
                SiteSearchResponsePaths.siteUpdatedAt,
                SiteSearchResponsePaths.mainAddressCreatedAt,
                SiteSearchResponsePaths.mainAddressUpdatedAt,
            )
            .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
            .isEqualTo(expected)
    }

    fun assertSiteVerbose(actual: PageDto<SiteVerboseV6Dto>, expected: PageDto<SiteVerboseV6Dto>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                "${PageDto<*>::content.name}.${SiteVerboseV6Dto::createdAt.name}",
                "${PageDto<*>::content.name}.${SiteVerboseV6Dto::updatedAt.name}",
            )
            .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
            .isEqualTo(expected)
    }

    fun assertSiteGet(actual: SiteWithMainAddressVerboseV6Dto, expected: SiteWithMainAddressVerboseV6Dto){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                SiteGetResponsePaths.siteCreatedAt,
                SiteGetResponsePaths.siteUpdatedAt,
                SiteGetResponsePaths.mainAddressCreatedAt,
                SiteGetResponsePaths.mainAddressUpdatedAt,
            )
            .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
            .isEqualTo(expected)
    }

    fun assertAdditionalAddressCreate(actual: AddressPartnerCreateV6ResponseWrapper, expected: AddressPartnerCreateV6ResponseWrapper){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields(
                AdditionalAddressUpsertPaths.bpnA,
                AdditionalAddressUpsertPaths.createdAt,
                AdditionalAddressUpsertPaths.updatedAt,
                AdditionalAddressUpsertPaths.errorMessage
            )
            .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
            .isEqualTo(expected)
    }
    fun assertAddressUpdate(actual: AddressPartnerUpdateV6ResponseWrapper, expected: AddressPartnerUpdateV6ResponseWrapper){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                AddressUpdatePaths.createdAt,
                AddressUpdatePaths.updatedAt,
                AddressUpdatePaths.errorMessage
            )
            .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
            .isEqualTo(expected)
    }

    fun assertAddressSearch(actual: PageDto<LogisticAddressVerboseV6Dto>, expected: PageDto<LogisticAddressVerboseV6Dto>){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                "${PageDto<*>::content.name}.${LogisticAddressVerboseV6Dto::createdAt.name}",
                "${PageDto<*>::content.name}.${LogisticAddressVerboseV6Dto::updatedAt.name}",
            )
            .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
            .isEqualTo(expected)
    }

    fun assertAddressGet(actual: LogisticAddressVerboseV6Dto, expected: LogisticAddressVerboseV6Dto){
        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                 LogisticAddressVerboseV6Dto::createdAt.name,
                 LogisticAddressVerboseV6Dto::updatedAt.name
            )
            .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
            .isEqualTo(expected)
    }


    private fun ObjectAssert<*>.usingRecursiveLegalEntityUpsertComparison(): RecursiveComparisonAssert<*>{
        return this.usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(
                LegalEntityUpsertResponsePaths.legalEntityCreatedAt,
                LegalEntityUpsertResponsePaths.legalEntityUpdatedAt,
                LegalEntityUpsertResponsePaths.legalAddressCreatedAt,
                LegalEntityUpsertResponsePaths.legalAddressUpdatedAt,
                LegalEntityUpsertResponsePaths.currentness,
                LegalEntityUpsertResponsePaths.bpnL,
                LegalEntityUpsertResponsePaths.bpnA,
                LegalEntityUpsertResponsePaths.bpnLegalEntity,
                LegalEntityUpsertResponsePaths.errorMessage
            )
            .withComparatorForType(localDatetimeSecondsComparator, LocalDateTime::class.java)
    }
}