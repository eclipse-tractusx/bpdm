package org.eclipse.tractusx.bpdm.pool.dto

import org.eclipse.tractusx.bpdm.pool.entity.IdentifierStatus
import org.eclipse.tractusx.bpdm.pool.entity.IdentifierType
import org.eclipse.tractusx.bpdm.pool.entity.IssuingBody
import org.eclipse.tractusx.bpdm.pool.entity.LegalForm

data class MetadataMappingDto (
    val idTypes: Map<String, IdentifierType>,
    val idStatuses: Map<String, IdentifierStatus>,
    val issuingBodies: Map<String, IssuingBody>,
    val legalForms: Map<String, LegalForm>
        )