/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.common.dto.openapidescription

object StreetDescription {
    const val header = "The street of the physical postal address, synonyms: road, avenue, lane, boulevard, highway"

    const val name = "The name of the street."
    const val houseNumber = "The number representing the exact location of a building within the street."
    const val milestone = "The number representing the exact location of an addressed object within a street without house numbers, such as within long roads."
    const val direction = "The cardinal direction describing where the exit to the location of the addressed object on large highways / " +
            "motorways is located, such as Highway 101 South."

    const val namePrefix = "The street related information, which is usually printed before the official street name on an address label."
    const val additionalNamePrefix = "The additional street related information, which is usually printed before the official street name on an address label."
    const val nameSuffix = "The street related information, which is usually printed after the official street name on an address label."
    const val additionalNameSuffix = "The additional street related information, which is usually printed after the official street name on an address label."
}
