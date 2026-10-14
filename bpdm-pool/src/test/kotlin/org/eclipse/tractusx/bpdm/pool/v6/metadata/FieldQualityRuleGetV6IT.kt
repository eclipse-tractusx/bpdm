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

package org.eclipse.tractusx.bpdm.pool.v6.metadata

import org.eclipse.tractusx.bpdm.pool.v6.UnscheduledPoolTestBaseV6
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.reactive.function.client.WebClientResponseException

class FieldQualityRuleGetV6IT : UnscheduledPoolTestBaseV6() {

    @Test
    fun `search field quality rules for a maintained country`() {
        //GIVEN
        val country = "DE"

        //WHEN
        val response = poolClient.metadata.getFieldQualityRules(country)

        //THEN
        assertRepository.assertFieldQualityRules(requireNotNull(response.body), country)
    }

    @Test
    fun `search field quality rules rejects an unknown country by name`() {
        //GIVEN
        val unknownCountry = "ZZ"

        //WHEN
        val error = assertThrows<WebClientResponseException.NotFound> {
            poolClient.metadata.getFieldQualityRules(unknownCountry)
        }

        //THEN
        assertRepository.assertNotFound(error, "Country with following identifiers not found: $unknownCountry")
    }
}
