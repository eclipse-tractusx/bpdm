````mermaid
classDiagram

PageResponseSiteResponse ..> SiteResponse
SiteResponse ..> SiteStateResponse
SiteStateResponse ..> TypeKeyNameDtoBusinessStateType

class PageResponseSiteResponse{
+number: totalElements
+number: totalPages
+number: page
+number: contentSize
+SiteResponse[]: content;
}

class SiteResponse{
+string: bpns
+string: name
+string: bpnLegalEntity
+date: createdAt
+date: updatedAt
+SiteStateResponse[]: states;
}

class SiteStateResponse{
+string: description
+date: validFrom
+date: validTo
+TypeKeyNameDtoBusinessStateType: type;
}

class TypeKeyNameDtoBusinessStateType{
+string: technicalKey
+string: name
}

````