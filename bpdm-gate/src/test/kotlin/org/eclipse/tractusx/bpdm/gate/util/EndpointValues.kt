package org.eclipse.tractusx.bpdm.gate.util

object EndpointValues {

    const val CDQ_MOCK_STORAGE_PATH = "/test-cdq-api/storages/test-cdq-storage"
    const val CDQ_MOCK_BUSINESS_PARTNER_PATH = "$CDQ_MOCK_STORAGE_PATH/businesspartners"
    const val CDQ_MOCK_FETCH_BUSINESS_PARTNER_PATH = "$CDQ_MOCK_BUSINESS_PARTNER_PATH/fetch"

    const val CATENA_PATH = "/api/catena"
    const val CATENA_INPUT_PATH = "${CATENA_PATH}/input"
    const val CATENA_INPUT_LEGAL_ENTITIES_PATH = "${CATENA_INPUT_PATH}/legal-entities"

    const val CATENA_OUTPUT_PATH = "${CATENA_PATH}/output"
    const val CATENA_OUTPUT_LEGAL_ENTITIES_PATH = "${CATENA_OUTPUT_PATH}/legal-entities"
}