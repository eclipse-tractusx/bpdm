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

package org.eclipse.tractusx.bpdm.common.dto.openapidescription

import org.eclipse.tractusx.bpdm.common.dto.BusinessStateDates

object BusinessStateDescription {
    const val validFrom = "The date from which the state is valid. " +
            "The earliest date ${BusinessStateDates.EARLIEST_TEXT} (UTC) stands for a state without start; " +
            "an earlier date counts as the earliest date."
    const val validTo = "The date until the state is valid. " +
            "The latest date ${BusinessStateDates.LATEST_TEXT} (UTC) stands for a state without end; " +
            "a later date counts as the latest date."
}
