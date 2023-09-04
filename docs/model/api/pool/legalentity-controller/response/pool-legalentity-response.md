````mermaid
classDiagram

LogisticAddressResponse ..> AddressStateResponse
LogisticAddressResponse ..> IdentifierResponse
LogisticAddressResponse ..> PhysicalPostalAddress
LogisticAddressResponse ..> AlternativePostalAddress
AlternativePostalAddress ..> GeoCoordinates
IdentifierResponse ..> TypeKeyNameDtoString
LegalEntityIdentifierResponse ..> TypeKeyNameDtoString
LegalEntityStateResponse ..> TypeKeyNameDtoBusinessStateType
PhysicalPostalAddress ..> GeoCoordinates
PhysicalPostalAddress ..> Street
PoolLegalEntityResponse ..> LegalEntityIdentifierResponse
PoolLegalEntityResponse ..> LegalFormResponse
PoolLegalEntityResponse ..> LegalEntityStateResponse
PoolLegalEntityResponse ..> ClassificationResponse
PoolLegalEntityResponse ..> RelationResponse
PoolLegalEntityResponse ..> LogisticAddressResponse
RelationResponse ..> TypeKeyNameDtoRelationType
AddressStateResponse ..> TypeKeyNameDtoBusinessStateType

class LogisticAddressResponse {
    +string: bpna
    +string: name
    +string: bpnLegalEntity
    +string: bpnSite
    +date: createdAt
    +date: updatedAt
    +bool: isLegalAddress
    +bool: isMainAddress
    +AddressStateResponse[]: states;
    +IdentifierResponse[]: identifiers;
    +PhysicalPostalAddress: physicalPostalAddress;
    +AlternativePostalAddress: alternativePostalAddress;
}

class AddressStateResponse {
    +string: description
    +date: validFrom
    +date: validTo
    +TypeKeyNameDtoBusinessStateType: type;
}

class IdentifierResponse {
    +string: value
    +TypeKeyNameDtoString: type;
}

class PhysicalPostalAddress {
    +string: country
    +string: postalCode
    +string: city
    +string: administrativeAreaLevel1
    +string: administrativeAreaLevel2
    +string: administrativeAreaLevel3
    +string: district
    +string: companyPostalCode
    +string: industrialZone
    +string: building
    +string: floor
    +string: door
    +GeoCoordinates: geographicCoordinates;
    +Street: street;
}

class AlternativePostalAddress {
    +string: country
    +string: postalCode
    +string: city
    +string: administrativeAreaLevel1
    +string: deliveryServiceNumber
    +string: deliveryServiceType
    +string: deliveryServiceQualifier
    +GeoCoordinates: geographicCoordinates;
}

class GeoCoordinates {
    +number: longitude
    +number: latitude
    +number: altitude
}

class TypeKeyNameDtoString {
    +string: technicalKey
    +string: name
}

class LegalEntityStateResponse {
    +string: officialDenotation
    +date: validFrom
    +date: validTo
    +TypeKeyNameDtoBusinessStateType: type;
}

class ClassificationResponse {
    +string: value
    +string: code
    +TypeKeyNameDtoClassificationType: type;
}

class RelationResponse {
    +string: startBpn
    +string: endBpn
    +date: validFrom
    +date: validTo
    +TypeKeyNameDtoRelationType: type;
}

class Street{
+string: name
+string: houseNumber
+string: milestone
+string: direction
}

class TypeKeyNameDtoBusinessStateType{
+string: technicalKey
+string: name
}

class TypeKeyNameDtoRelationType{
+string: technicalKey
+string: name
}

class LegalEntityIdentifierResponse{
+string: value
+string: issuingBody
+TypeKeyNameDtoString: type;
}

class LegalFormResponse{
+string: technicalKey
+string: name
+string: abbreviation
}

class PoolLegalEntityResponse{
+string: legalName
+string: bpnl
+string: legalShortName
+date: currentness
+date: createdAt
+date: updatedAt
+LegalEntityIdentifierResponse[]: identifiers;
+LegalFormResponse: legalForm;
+LegalEntityStateResponse[]: states;
+ClassificationResponse[]: classifications;
+RelationResponse[]: relations;
+LogisticAddressResponse: legalAddress;
}

````