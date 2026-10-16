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

package org.eclipse.tractusx.bpdm.gate.model.businesspartner

import org.eclipse.tractusx.bpdm.common.logging.PersistedChange
import org.eclipse.tractusx.bpdm.common.logging.PersistedChangeSubject
import org.eclipse.tractusx.bpdm.common.logging.PersistedChangeType

data class BusinessPartnerChange(
    override val type: PersistedChangeType,
    val externalId: String,
    val tenantBpnl: String?,
    val aspect: Aspect
) : PersistedChange {

    override val subject = SUBJECT

    override fun identifierForLog() = tenantBpnl?.let { "$externalId@$it" } ?: externalId

    override fun aspectForLog() = aspect.logName

    enum class Aspect(val logName: String) {
        Input("input"),
        SharingState("sharing state")
    }

    private companion object {
        val SUBJECT = PersistedChangeSubject("business partner", "business partners")
    }
}
