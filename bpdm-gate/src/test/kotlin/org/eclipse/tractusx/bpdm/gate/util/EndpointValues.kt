package org.eclipse.tractusx.bpdm.gate.util

object EndpointValues {

    const val CDQ_MOCK_STORAGE_PATH = "/test-cdq-api/storages/test-cdq-storage"
    const val CDQ_MOCK_BUSINESS_PARTNER_PATH = "$CDQ_MOCK_STORAGE_PATH/businesspartners"
    const val CDQ_MOCK_FETCH_BUSINESS_PARTNER_PATH = "$CDQ_MOCK_BUSINESS_PARTNER_PATH/fetch"

    const val CATENA_PATH = "/api/catena"
    const val CATENA_LEGAL_ENTITIES_PATH = "${CATENA_PATH}/legal-entities"
}