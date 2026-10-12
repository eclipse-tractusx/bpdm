/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.bpdm.pool.v7.metadata

import org.eclipse.tractusx.bpdm.pool.api.model.FieldQualityRuleDto
import org.eclipse.tractusx.bpdm.pool.api.model.QualityLevel
import org.eclipse.tractusx.bpdm.pool.v7.UnscheduledPoolTestBaseV7
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.reactive.function.client.WebClientResponseException

class FieldQualityRuleGetV7IT : UnscheduledPoolTestBaseV7() {

    /**
     * GIVEN the German field quality rules populated by Flyway
     * WHEN a client requests field quality rules for DE
     * THEN the response contains the exact maintained address rules
     */
    @Test
    fun `get field quality rules for Germany`() {
        //GIVEN
        val country = "DE"
        val expectedRules = germanAddressRules.map { (fieldPath, qualityLevel) ->
            FieldQualityRuleDto(fieldPath, "address", country, qualityLevel)
        }

        //WHEN
        val response = requireNotNull(poolClient.metadata.getFieldQualityRules(country).body)

        //THEN
        assertRepository.assertFieldQualityRules(response, expectedRules)
    }

    /**
     * GIVEN ZZ is not a maintained country code
     * WHEN a client requests field quality rules for ZZ
     * THEN the endpoint returns Not Found naming both Country and ZZ
     */
    @Test
    fun `get field quality rules for an unknown country returns not found`() {
        //GIVEN
        val unknownCountry = "ZZ"

        //WHEN
        val error = assertThrows<WebClientResponseException.NotFound> {
            poolClient.metadata.getFieldQualityRules(unknownCountry)
        }

        //THEN
        assertRepository.assertNotFound(error, "Country with following identifiers not found: $unknownCountry")
    }

    private val germanAddressRules = listOf(
        "physicalPostalAddress.administrativeAreaLevel1" to QualityLevel.OPTIONAL,
        "physicalPostalAddress.administrativeAreaLevel2" to QualityLevel.OPTIONAL,
        "physicalPostalAddress.administrativeAreaLevel3" to QualityLevel.OPTIONAL,
        "physicalPostalAddress.administrativeAreaLevel4" to QualityLevel.OPTIONAL,
        "physicalPostalAddress.city" to QualityLevel.MANDATORY,
        "physicalPostalAddress.country" to QualityLevel.MANDATORY,
        "physicalPostalAddress.districtLevel2" to QualityLevel.OPTIONAL,
        "physicalPostalAddress.postCode" to QualityLevel.MANDATORY,
        "physicalPostalAddress.street.houseNumber" to QualityLevel.OPTIONAL,
        "physicalPostalAddress.street.milestone" to QualityLevel.OPTIONAL,
        "physicalPostalAddress.street.name" to QualityLevel.MANDATORY,
        "alternativePostalAddress.administrativeAreaLevel1" to QualityLevel.OPTIONAL,
        "alternativePostalAddress.administrativeAreaLevel2" to QualityLevel.OPTIONAL,
        "alternativePostalAddress.administrativeAreaLevel3" to QualityLevel.OPTIONAL,
        "alternativePostalAddress.administrativeAreaLevel4" to QualityLevel.OPTIONAL,
        "alternativePostalAddress.city" to QualityLevel.MANDATORY,
        "alternativePostalAddress.country" to QualityLevel.MANDATORY,
        "alternativePostalAddress.districtLevel2" to QualityLevel.OPTIONAL,
        "alternativePostalAddress.postCode" to QualityLevel.MANDATORY,
        "alternativePostalAddress.street.houseNumber" to QualityLevel.OPTIONAL,
        "alternativePostalAddress.street.milestone" to QualityLevel.OPTIONAL,
        "alternativePostalAddress.street.name" to QualityLevel.MANDATORY
    )
}
