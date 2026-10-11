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

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.pool.api.model.CountryDto
import org.eclipse.tractusx.bpdm.pool.v6.UnscheduledPoolTestBaseV6
import org.junit.jupiter.api.Test

class CountryGetV6IT : UnscheduledPoolTestBaseV6() {

    @Test
    fun `search country metadata returns maintained country codes and names`() {
        val firstPage = poolClient.metadata.getCountries(PaginationRequest(size = 100))
        val countries = firstPage.content + (1 until firstPage.totalPages).flatMap {
            poolClient.metadata.getCountries(PaginationRequest(page = it, size = 100)).content
        }

        assertThat(countries).contains(CountryDto("DE", "Germany", null))
        assertThat(countries).allSatisfy {
            assertThat(it.countryCode).isNotBlank()
            assertThat(it.name).isNotBlank()
        }
        assertThat(countries).hasSize(firstPage.totalElements.toInt())
    }

    @Test
    fun `search country metadata respects pagination`() {
        val firstPage = poolClient.metadata.getCountries(PaginationRequest(page = 0, size = 1))
        val secondPage = poolClient.metadata.getCountries(PaginationRequest(page = 1, size = 1))

        assertThat(firstPage.content).hasSize(1)
        assertThat(secondPage.content).hasSize(1)
        assertThat(firstPage.totalElements).isGreaterThan(1)
        assertThat(secondPage.page).isEqualTo(1)
        assertThat(secondPage.content.single().countryCode).isNotEqualTo(firstPage.content.single().countryCode)
    }
}
