````mermaid
classDiagram

PageResponseSharingState ..> SharingState

class SharingState{
+string: businessPartnerType
+string: externalId
+string: sharingStateType
+string: sharingErrorCode
+string: sharingErrorMessage
+string: bpn
+date: sharingProcessStarted
}
class PageResponseSharingState{
+number: totalElements
+number: totalPages
+number: page
+number: contentSize
+SharingState[]: content;
}
````