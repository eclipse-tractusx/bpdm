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

package org.eclipse.tractusx.orchestrator.api.model

import org.eclipse.tractusx.bpdm.common.dto.IPageDto
import java.time.Instant

data class FinishedTaskEventsResponse(
    override val totalElements: Long,
    override val totalPages: Int,
    override val page: Int,
    override val contentSize: Int,
    override val content: Collection<Event>
): IPageDto<FinishedTaskEventsResponse.Event>{

    data class Event(
        val timestamp: Instant,
        val resultState: ResultState,
        val taskId: String
    )
}