package com.catenax.gpdm.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class BpdmElasticIndexException(
    detailMessage: String
) : RuntimeException("Exception occured when manipulating Index of Elasticsearch: $detailMessage")