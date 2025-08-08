/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.service.validation.validator.impl

import org.eclipse.tractusx.bpdm.pool.dto.input.GeoData
import org.eclipse.tractusx.bpdm.pool.dto.valid.GeoDataValid
import org.eclipse.tractusx.bpdm.pool.dto.validation.ValidatedOptional
import org.eclipse.tractusx.bpdm.pool.dto.validation.error.GeoDataError
import org.eclipse.tractusx.bpdm.pool.dto.validation.error.IsMissingError
import org.eclipse.tractusx.bpdm.pool.service.validation.validator.GeoDataValidator
import org.eclipse.tractusx.bpdm.pool.util.letNonNull
import org.springframework.stereotype.Service

@Service
class GeoDataValidatorImpl: GeoDataValidator {

    companion object{
        private val nullObject = GeoData(null, null, null)
        private val isMissingError = IsMissingError()
        private val latitudeError =  GeoDataError.Latitude(isMissingError)
        private val longitudeError = GeoDataError.Longitude(isMissingError)
    }

    override fun validate(geoCoordinates: List<GeoData>): List<ValidatedOptional<GeoDataValid, GeoDataError>> {
        return geoCoordinates
            .map { normalize(it) }
            .map { geoData ->
                if(geoData == null) return@map ValidatedOptional(null, emptySet())

                val latitudeError = if(geoData.latitude == null) latitudeError else null
                val longitudeError = if(geoData.longitude == null) longitudeError else null

                val errors = setOfNotNull(latitudeError, longitudeError)

                val validatedValue =  letNonNull(geoData.latitude, geoData.longitude){ lat, long ->
                    GeoDataValid(
                        lat,
                        long,
                        geoData.altitude
                    )
                }

                ValidatedOptional(validatedValue, errors)
        }
    }

    private fun normalize(geoData: GeoData): GeoData?{
        return if(geoData == nullObject) null else geoData
    }
}