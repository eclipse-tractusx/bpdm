/*******************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.component.opensearch.impl.doc

enum class SuggestionType(val docName: String) {
    NAME(BusinessPartnerDoc::names.name),
    LEGAL_FORM(BusinessPartnerDoc::legalForm.name),
    STATUS(BusinessPartnerDoc::status.name),
    CLASSIFICATION(BusinessPartnerDoc::classifications.name),
    ADMIN_AREA("${BusinessPartnerDoc::addresses.name}.${AddressDoc::administrativeAreas.name}"),
    POSTCODE("${BusinessPartnerDoc::addresses.name}.${AddressDoc::postCodes.name}"),
    LOCALITY("${BusinessPartnerDoc::addresses.name}.${AddressDoc::localities.name}"),
    THOROUGHFARE("${BusinessPartnerDoc::addresses.name}.${AddressDoc::thoroughfares.name}"),
    PREMISE("${BusinessPartnerDoc::addresses.name}.${AddressDoc::premises.name}"),
    POSTAL_DELIVERY_POINT("${BusinessPartnerDoc::addresses.name}.${AddressDoc::postalDeliveryPoints.name}"),
    SITE(BusinessPartnerDoc::sites.name),
}