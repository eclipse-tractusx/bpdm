package com.catenax.gpdm.component.elastic.impl.doc

enum class SuggestionType(val docName: String) {
    NAME(BusinessPartnerDoc::names.name),
    LEGAL_FORM(BusinessPartnerDoc::legalForm.name),
    STATUS(BusinessPartnerDoc::status.name),
    CLASSIFICATION(BusinessPartnerDoc::classifications.name),
    ADMIN_AREA("${BusinessPartnerDoc::addresses.name}.${AddressDoc::administrativeAreas.name}"),
    POSTCODE("${BusinessPartnerDoc::addresses.name}.${AddressDoc::postCodes.name}"),
    LOCALITY("${BusinessPartnerDoc::addresses.name}.${AddressDoc::localities.name}"),
    THOROUGHFARE("${BusinessPartnerDoc::addresses.name}.${AddressDoc::thoroughfares.name}"),
    PREMISE("${BusinessPartnerDoc::addresses.name}.${AddressDoc::premises.name}"),
    POSTAL_DELIVERY_POINT("${BusinessPartnerDoc::addresses.name}.${AddressDoc::postalDeliveryPoints.name}"),
    SITE(BusinessPartnerDoc::sites.name),
}