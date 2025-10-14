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

package org.eclipse.tractusx.bpdm.gate.repository.generic

import org.eclipse.tractusx.bpdm.gate.entity.generic.ConfidenceCriteriaDb
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface ConfidenceCriteriaRepository : JpaRepository<ConfidenceCriteriaDb, Long>, CrudRepository<ConfidenceCriteriaDb, Long> {

    @Query("SELECT AVG(c.numberOfBusinessPartners) FROM ConfidenceCriteriaDb AS c")
    fun averageNumberOfBusinessPartners(): Float?

    @Query("SELECT COUNT(c.numberOfBusinessPartners) FROM ConfidenceCriteriaDb AS c WHERE c.numberOfBusinessPartners <= 1")
    fun countUnique(): Long?

    @Query("SELECT COUNT(c.sharedByOwner) FROM ConfidenceCriteriaDb AS c")
    fun countSharedByOwner(): Long?

    @Query("SELECT COUNT(c.checkedByExternalDataSource) FROM ConfidenceCriteriaDb AS c")
    fun countCheckedByExternalDataSource(): Long?

    @Query("SELECT AVG(c.confidenceLevel) FROM ConfidenceCriteriaDb AS c")
    fun averageConfidenceLevel(): Float?

}