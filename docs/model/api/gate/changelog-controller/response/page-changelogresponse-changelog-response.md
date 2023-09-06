````mermaid
classDiagram

PageChangeLogResponseChangelogResponse ..> ChangelogResponse
PageChangeLogResponseChangelogResponse ..> ErrorInfoChangeLogOutputError

class ChangelogResponse{
+string: externalId
+string: businessPartnerType
+date: timestamp
+string: changelogType 
}
class ErrorInfoChangeLogOutputError{
+string: errorCode
+string: message
+string: entityKey
}
class PageChangeLogResponseChangelogResponse{
+number: totalElements
+number: totalPages
+number: page
+number: contentSize
+number: invalidEntries
+ChangelogResponse[]: content;
+ErrorInfoChangeLogOutputError[]: errors;
}

````