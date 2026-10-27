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

package org.eclipse.tractusx.bpdm.gate.model.task

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.entity.RelationDb

/**
 * Reason a relation staged for sharing cannot become a golden record task.
 */
sealed interface RelationTaskPreparationError {
    val relation: RelationDb
    val message: String

    data class CombinationUnsupported(
        override val relation: RelationDb,
        val relationType: RelationType,
        val sourceAddressType: AddressType?,
        val targetAddressType: AddressType?
    ) : RelationTaskPreparationError {
        override val message =
            "Unsupported relation combination for relation: relationType='$relationType', " +
                    "sourceAddressType='$sourceAddressType', targetAddressType='$targetAddressType'"
    }

    data class BpnMissing(
        override val relation: RelationDb,
        val bpnType: String,
        val sourceBpn: String?,
        val targetBpn: String?
    ) : RelationTaskPreparationError {
        override val message =
            "Relation task requires $bpnType for both sides but missing for relation: source$bpnType='$sourceBpn', target$bpnType='$targetBpn'"
    }
}
