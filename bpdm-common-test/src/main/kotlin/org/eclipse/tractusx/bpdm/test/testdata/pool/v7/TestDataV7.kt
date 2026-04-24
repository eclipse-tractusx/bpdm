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

package org.eclipse.tractusx.bpdm.test.testdata.pool.v7

import java.time.LocalDateTime

object TestDataV7 {

    val anyTime: LocalDateTime = LocalDateTime.now()

    val NoConfidence = CalculatedConfidence(
        numberOfSharingMembers = 0,
        confidenceLevel = 0
    )

    val SharedByOwnerConfidence = CalculatedConfidence(
        numberOfSharingMembers = 0,
        confidenceLevel = 5
    )

    val DefaultSiteConfidence = CalculatedConfidence(
        numberOfSharingMembers = 1,
        confidenceLevel = 5
    )

    val NotCheckedNotOwned = GivenConfidence(
        sharedByOwner = false,
        checkedByExternalDataSource = false
    )

    val IsShared = GivenConfidence(
        sharedByOwner = true,
        checkedByExternalDataSource = false
    )

}