package com.catenax.gpdm.dto

import com.catenax.gpdm.entity.IdentifierStatus
import com.catenax.gpdm.entity.IdentifierType
import com.catenax.gpdm.entity.IssuingBody
import com.catenax.gpdm.entity.LegalForm

data class MetadataMappingDto (
    val idTypes: Map<String, IdentifierType>,
    val idStatuses: Map<String, IdentifierStatus>,
    val issuingBodies: Map<String, IssuingBody>,
    val legalForms: Map<String, LegalForm>
        )