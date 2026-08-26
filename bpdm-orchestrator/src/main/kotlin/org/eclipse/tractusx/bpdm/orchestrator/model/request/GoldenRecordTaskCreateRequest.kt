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

package org.eclipse.tractusx.bpdm.orchestrator.model.request

import org.eclipse.tractusx.orchestrator.api.model.BusinessPartner

/**
 * The unified per-entry request for creating a golden record task, shared by the V6 and V7 API versions.
 * The business partner content itself already uses the V7 model as its common structure since V7's model
 * is a structural superset of V6's (V6's nested types like [org.eclipse.tractusx.orchestrator.api.model.Site]
 * or [org.eclipse.tractusx.orchestrator.api.model.PostalAddress] are in fact the very same classes reused by
 * V6, only the top-level `BusinessPartner`/`LegalEntity` shapes differ slightly between the two API versions).
 */
data class GoldenRecordTaskCreateRequest(
    val recordId: String?,
    val businessPartner: BusinessPartner
)
