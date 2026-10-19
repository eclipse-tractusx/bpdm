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


package org.eclipse.tractusx.bpdm.gate.model.relation

/**
 * Which of the requested relations an input upsert may create and which it may update.
 */
enum class RelationInputUpsertMode {
    CreateOrUpdate,
    UpdateOnly,
    CreateOnly
}
