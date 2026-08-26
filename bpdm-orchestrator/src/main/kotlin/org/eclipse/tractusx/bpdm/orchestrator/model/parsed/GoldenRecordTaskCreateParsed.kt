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

package org.eclipse.tractusx.bpdm.orchestrator.model.parsed

import org.eclipse.tractusx.bpdm.orchestrator.entity.SharingMemberRecordDb
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartner

/**
 * The validated, per-entry result of a golden record task create request.
 *
 * [existingGateRecord] is the already-persisted gate record the caller referred to by `recordId`, resolved
 * (read-only) by the parser. `null` means no `recordId` was given, so a new gate record still needs to be
 * created for this entry - that creation is a write and therefore happens during execution, not parsing.
 */
data class GoldenRecordTaskCreateParsed(
    val existingGateRecord: SharingMemberRecordDb?,
    val businessPartner: BusinessPartner
)
