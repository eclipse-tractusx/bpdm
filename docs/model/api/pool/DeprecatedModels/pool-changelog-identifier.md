```mermaid
classDiagram

IdentifierTypeDto ..> IdentifierTypeDetailDto
PageResponseIdentifierTypeDto ..> IdentifierTypeDto
PageResponseChangelogEntryResponse ..> ChangelogEntryResponse

    note for PageResponseIdentifierTypeDto "/api/catena/identifier-types [GET] [Response]"

    note for BpnIdentifierMappingResponse "/api/catena/bpn/search [POST] [requestBody]"

    note for ChangeLogSearchRequest "/api/catena/business-partners/changelog/search [POST] [requestBody]"

    note for FieldQualityRuleDto  "/api/catena/field-quality-rules/ [GET] [Response]"

    note for IdentifiersSearchRequest "/api/catena/bpn/search [POST] [requestBody]"

    note for LegalFormRequest "/api/catena/legal-forms [POST] [requestBody]"

    note for SyncResponse "/api/opensearch/business-partner [POST] [Response]"
    note for SyncResponse "/api/opensearch/business-partner [get] [Response]"

    note for PageResponseChangelogEntryResponse "/api/catena/business-partners/changelog/search [POST] [Response]"

class BpnIdentifierMappingResponse{
+string: idValue
+string: bpn
}
class ChangeLogSearchRequest{
+date: fromTime
+string[]: bpns
+string[]: lsaTypes
}
class FieldQualityRuleDto{
+string: fieldPath
+string: schemaName
+string: country
+string: qualityLevel
}
class IdentifiersSearchRequest{
+string: lsaType
+string: idType
+string[]: idValues
}
class LegalFormRequest{
+string: technicalKey
+string: name
+string: abbreviation
}
class SyncResponse{
+string: type
+string: status
+number: count
+number: progress
+string: errorDetails
+date: startedAt
+date: finishedAt
}
class PageResponseIdentifierTypeDto{
+number: totalElements
+number: totalPages
+number: page
+number: contentSize
+IdentifierTypeDto[]: content;
}
class IdentifierTypeDetailDto{
+string: country
+bool: mandatory
}
class IdentifierTypeDto{
+string: technicalKey
+string: lsaType
+string: name
+IdentifierTypeDetailDto[]: details;
}
class ChangelogEntryResponse{
+string: bpn
+string: changelogType
+date: timestamp
+string: lsaType
}
class PageResponseChangelogEntryResponse{
+number: totalElements
+number: totalPages
+number: page
+number: contentSize
+ChangelogEntryResponse[]: content;
}
´´´