````mermaid
classDiagram

PageResponseChangelogEntryResponse ..> ChangelogEntryResponse

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
````