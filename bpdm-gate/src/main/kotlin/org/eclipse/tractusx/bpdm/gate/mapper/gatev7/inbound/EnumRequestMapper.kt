/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.bpdm.gate.mapper.gatev7.inbound

import org.eclipse.tractusx.bpdm.gate.model.relation.RelationSharingStateType
import org.eclipse.tractusx.bpdm.gate.model.relation.RelationType
import org.eclipse.tractusx.bpdm.gate.model.relation.SharableRelationType
import org.eclipse.tractusx.bpdm.gate.model.sharingstate.SharingStateType
import org.springframework.stereotype.Component
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateType as RelationSharingStateTypeDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType as RelationTypeDto
import org.eclipse.tractusx.bpdm.gate.api.model.SharableRelationType as SharableRelationTypeDto
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType as SharingStateTypeDto

/**
 * Maps the enumerated values the API receives to the Gate's own.
 */
@Component
class EnumRequestMapper {

    /**
     * Returns the Gate's sharing state type for [sharingStateType].
     */
    fun toModel(sharingStateType: SharingStateTypeDto): SharingStateType =
        when (sharingStateType) {
            SharingStateTypeDto.Pending -> SharingStateType.Pending
            SharingStateTypeDto.Success -> SharingStateType.Success
            SharingStateTypeDto.Error -> SharingStateType.Error
            SharingStateTypeDto.Initial -> SharingStateType.Initial
            SharingStateTypeDto.Ready -> SharingStateType.Ready
        }

    /**
     * Returns the Gate's relation input type for [relationType].
     */
    fun toModel(relationType: RelationTypeDto): RelationType =
        when (relationType) {
            RelationTypeDto.IsManagedBy -> RelationType.IsManagedBy
            RelationTypeDto.IsAlternativeHeadquarterFor -> RelationType.IsAlternativeHeadquarterFor
            RelationTypeDto.IsOwnedBy -> RelationType.IsOwnedBy
            RelationTypeDto.IsReplacedBy -> RelationType.IsReplacedBy
        }

    /**
     * Returns the Gate's relation output type for [relationType].
     */
    fun toModel(relationType: SharableRelationTypeDto): SharableRelationType =
        when (relationType) {
            SharableRelationTypeDto.IsAlternativeHeadquarterFor -> SharableRelationType.IsAlternativeHeadquarterFor
            SharableRelationTypeDto.IsManagedBy -> SharableRelationType.IsManagedBy
            SharableRelationTypeDto.IsOwnedBy -> SharableRelationType.IsOwnedBy
            SharableRelationTypeDto.IsReplacedBy -> SharableRelationType.IsReplacedBy
        }

    /**
     * Returns the Gate's relation sharing state type for [sharingStateType].
     */
    fun toModel(sharingStateType: RelationSharingStateTypeDto): RelationSharingStateType =
        when (sharingStateType) {
            RelationSharingStateTypeDto.Ready -> RelationSharingStateType.Ready
            RelationSharingStateTypeDto.Pending -> RelationSharingStateType.Pending
            RelationSharingStateTypeDto.Error -> RelationSharingStateType.Error
            RelationSharingStateTypeDto.Success -> RelationSharingStateType.Success
        }
}
