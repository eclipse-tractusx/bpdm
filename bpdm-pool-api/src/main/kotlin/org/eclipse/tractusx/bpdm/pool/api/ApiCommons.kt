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

package org.eclipse.tractusx.bpdm.pool.api

object ApiCommons {

    const val BASE_PATH = "v6"

    const val LEGAL_ENTITIES_NAME = "Legal Entity Controller"
    const val LEGAL_ENTITIES_DESCRIPTION = "Read, create and update business partner of type legal entity"

    const val SITE_NAME = "Site Controller"
    const val SITE_DESCRIPTION ="Read, create and update business partner of type site"

    const val ADDRESS_NAME = "Address Controller"
    const val ADDRESS_DESCRIPTION = "Read, create and update business partner of type address"

    const val METADATA_NAME = "Metadata Controller"
    const val METADATA_DESCRIPTION = "Read and create supporting data that is referencable in business partner data"

    const val CHANGELOG_NAME = "Changelog Controller"
    const val CHANGELOG_DESCRIPTION = "Read change events of business partner data"

    const val BPN_NAME = "Bpn Controller"
    const val BPN_DESCRIPTION = "Support functionality for BPN operations"


}