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

package org.eclipse.tractusx.bpdm.gate.exception

import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerInputUpsertError

class BpdmBusinessPartnerInputUpsertException(
    val errors: List<BusinessPartnerInputUpsertError>
) : RuntimeException("Business partner input upsert rejected: ${errors.joinToString("; ") { it.message }}")
