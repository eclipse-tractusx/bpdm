classDiagram

LogisticAddressResponse ..> AddressStateResponse LogisticAddressResponse ..> IdentifierResponse LogisticAddressResponse ..> PhysicalPostalAddress
LogisticAddressResponse ..> AlternativePostalAddress AlternativePostalAddress ..> GeoCoordinates ClassificationResponse ..> TypeKeyNameDtoClassificationType
IdentifierResponse ..> TypeKeyNameDtoString LegalEntityCreateWrapper ..> LegalEntityPartnerCreateResponse LegalEntityCreateWrapper ..>
ErrorInfoLegalEntityCreateError LegalEntityIdentifierResponse ..> TypeKeyNameDtoString LegalEntityMatchResponse ..> LegalEntityIdentifierResponse
LegalEntityMatchResponse ..> LegalFormResponse LegalEntityMatchResponse ..> LegalEntityStateResponse LegalEntityMatchResponse ..> ClassificationResponse
LegalEntityMatchResponse ..> RelationResponse LegalEntityMatchResponse ..> LogisticAddressResponse LegalEntityPartnerCreateRequest ..> LegalEntityIdentifier
LegalEntityPartnerCreateRequest ..> LegalEntityState LegalEntityPartnerCreateRequest ..> Classification LegalEntityPartnerCreateRequest ..> LogisticAddressDto
LegalEntityPartnerCreateResponse ..> LegalEntityIdentifierResponse LegalEntityPartnerCreateResponse ..> LegalFormResponse LegalEntityPartnerCreateResponse ..>
LegalEntityStateResponse LegalEntityPartnerCreateResponse ..> ClassificationResponse LegalEntityPartnerCreateResponse ..> RelationResponse
LegalEntityPartnerCreateResponse ..> LogisticAddressResponse LegalEntityPartnerUpdateRequest ..> LegalEntityIdentifier LegalEntityPartnerUpdateRequest ..>
LegalEntityState LegalEntityPartnerUpdateRequest ..> Classification LegalEntityPartnerUpdateRequest ..> LogisticAddressDto LegalEntityStateResponse ..>
TypeKeyNameDtoBusinessStateType LegalEntityUpdateWrapper ..> LegalEntityPartnerCreateResponse LegalEntityUpdateWrapper ..> ErrorInfoLegalEntityUpdateError
LogisticAddressDto ..> AddressState LogisticAddressDto ..> AddressIdentifier LogisticAddressDto ..> PhysicalPostalAddress LogisticAddressDto ..>
AlternativePostalAddress PageResponseLegalEntityMatchResponse ..> LegalEntityMatchResponse PageResponseLegalFormResponse ..> LegalFormResponse
PhysicalPostalAddress ..> GeoCoordinates PhysicalPostalAddress ..> Street PoolLegalEntityResponse ..> LegalEntityIdentifierResponse PoolLegalEntityResponse ..>
LegalFormResponse PoolLegalEntityResponse ..> LegalEntityStateResponse PoolLegalEntityResponse ..> ClassificationResponse PoolLegalEntityResponse ..>
RelationResponse PoolLegalEntityResponse ..> LogisticAddressResponse RelationResponse ..> TypeKeyNameDtoRelationType AddressStateResponse ..>
TypeKeyNameDtoBusinessStateType

    note for PageResponseLegalEntityMatchResponse "/api/catena/legal-entities [GET] [Response]"

    note for LegalEntityCreateWrapper "/api/catena/legal-entities [POST] [Response]"

    note for PoolLegalEntityResponse "/api/catena/legal-entities/search [POST] [Response]"

    note for LegalEntityUpdateWrapper "/api/catena/legal-entities [PUT] [Response]"

    note for PageResponseLegalFormResponse "/api/catena/legal-forms [GET] [Response]"

    note for LegalEntityPartnerCreateRequest "/api/catena/legal-entities [POST] [requestBody]"

    note for LegalEntityPartnerUpdateRequest "/api/catena/legal-entities [PUT] [requestBody]"

class LogisticAddressResponse{ +string: bpna +string: name +string: bpnLegalEntity +string: bpnSite +date: createdAt +date: updatedAt +bool: isLegalAddress
+bool: isMainAddress +AddressStateResponse[]: states; +IdentifierResponse[]: identifiers; +PhysicalPostalAddress: physicalPostalAddress;
+AlternativePostalAddress: alternativePostalAddress; } class AddressIdentifier{ +string: value +string: type } class AddressState{ +string: description +date:
validFrom +date: validTo +string: type } class AlternativePostalAddress{ +string: country +string: postalCode +string: city +string: administrativeAreaLevel1
+string: deliveryServiceNumber +string: deliveryServiceType +string: deliveryServiceQualifier +GeoCoordinates: geographicCoordinates; } class Classification{
+string: value +string: code +string: type } class ClassificationResponse{ +string: value +string: code +TypeKeyNameDtoClassificationType: type; } class
ErrorInfoLegalEntityCreateError{ +string: errorCode +string: message +string: entityKey } class ErrorInfoLegalEntityUpdateError{ +string: errorCode +string:
message +string: entityKey } class GeoCoordinates{ +number: longitude +number: latitude +number: altitude } class IdentifierResponse{ +string: value
+TypeKeyNameDtoString: type; } class LegalEntityCreateWrapper{ +number: errorCount +number: entityCount +LegalEntityPartnerCreateResponse[]: entities;
+ErrorInfoLegalEntityCreateError[]: errors; } class LegalEntityIdentifier{ +string: value +string: type +string: issuingBody } class
LegalEntityIdentifierResponse{ +string: value +string: issuingBody +TypeKeyNameDtoString: type; } class LegalEntityMatchResponse{ +number: score +string:
legalName +string: bpnl +string: legalShortName +date: currentness +date: createdAt +date: updatedAt +LegalEntityIdentifierResponse[]: identifiers;
+LegalFormResponse: legalForm; +LegalEntityStateResponse[]: states; +ClassificationResponse[]: classifications; +RelationResponse[]: relations;
+LogisticAddressResponse: legalAddress; } class LegalEntityPartnerCreateRequest{ +string: legalName +string: legalShortName +string: legalForm +string: index
+LegalEntityIdentifier[]: identifiers; +LegalEntityState[]: states; +Classification[]: classifications; +LogisticAddressDto: legalAddress; } class
LegalEntityPartnerCreateResponse{ +string: legalName +string: bpnl +string: legalShortName +date: currentness +date: createdAt +date: updatedAt +string: index
+LegalEntityIdentifierResponse[]: identifiers; +LegalFormResponse: legalForm; +LegalEntityStateResponse[]: states; +ClassificationResponse[]: classifications;
+RelationResponse[]: relations; +LogisticAddressResponse: legalAddress; } class LegalEntityPartnerUpdateRequest{ +string: bpnl +string: legalName +string:
legalShortName +string: legalForm +LegalEntityIdentifier[]: identifiers; +LegalEntityState[]: states; +Classification[]: classifications; +LogisticAddressDto:
legalAddress; } class LegalEntityState{ +string: officialDenotation +date: validFrom +date: validTo +string: type } class LegalEntityStateResponse{ +string:
officialDenotation +date: validFrom +date: validTo +TypeKeyNameDtoBusinessStateType: type; } class LegalEntityUpdateWrapper{ +number: errorCount +number:
entityCount +LegalEntityPartnerCreateResponse[]: entities; +ErrorInfoLegalEntityUpdateError[]: errors; } class LegalFormResponse{ +string: technicalKey +string:
name +string: abbreviation } class LogisticAddressDto{ +string: name +AddressState[]: states; +AddressIdentifier[]: identifiers; +PhysicalPostalAddress:
physicalPostalAddress; +AlternativePostalAddress: alternativePostalAddress; } class PageResponseLegalEntityMatchResponse{ +number: totalElements +number:
totalPages +number: page +number: contentSize +LegalEntityMatchResponse[]: content; } class PageResponseLegalFormResponse{ +number: totalElements +number:
totalPages +number: page +number: contentSize +LegalFormResponse[]: content; } class PhysicalPostalAddress{ +string: country +string: postalCode +string: city
+string: administrativeAreaLevel1 +string: administrativeAreaLevel2 +string: administrativeAreaLevel3 +string: district +string: companyPostalCode +string:
industrialZone +string: building +string: floor +string: door +GeoCoordinates: geographicCoordinates; +Street: street; } class PoolLegalEntityResponse{ +string:
legalName +string: bpnl +string: legalShortName +date: currentness +date: createdAt +date: updatedAt +LegalEntityIdentifierResponse[]: identifiers;
+LegalFormResponse: legalForm; +LegalEntityStateResponse[]: states; +ClassificationResponse[]: classifications; +RelationResponse[]: relations;
+LogisticAddressResponse: legalAddress; } class RelationResponse{ +string: startBpn +string: endBpn +date: validFrom +date: validTo +TypeKeyNameDtoRelationType:
type; } class Street{ +string: name +string: houseNumber +string: milestone +string: direction } class TypeKeyNameDtoBusinessStateType{ +string: technicalKey
+string: name } class TypeKeyNameDtoClassificationType{ +string: technicalKey +string: name } class TypeKeyNameDtoRelationType{ +string: technicalKey +string:
name } class TypeKeyNameDtoString{ +string: technicalKey +string: name } class AddressStateResponse{ +string: description +date: validFrom +date: validTo
+TypeKeyNameDtoBusinessStateType: type; }