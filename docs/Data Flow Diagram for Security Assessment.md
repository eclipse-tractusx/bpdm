## Data Flow Diagram BPDM Golden Record

Below diagram was designed with purpose to pull it with the security assessment documentation crated by Kristian - https://github.com/eclipse-tractusx/bpdm/pull/737/commits/ac5ee908bb0cc447833dc14143e2acd512b99eb3

```mermaid
flowchart LR
    A(Operator) 
    B(EDC CX Member)
    AA(CX SME \n Human User)
    BB(EDC CX Member)
    AAA(CX Member \n Master Data Management System)
    BBB(EDC CX Member)

    A -->|Authentication & Authorization \n Business Partner Data consuming digital services or data assets \n HTTPS Protocol| B 
    AA -->|Authentication & Authorization \n Business Partner Data, consuming digital services or data assets \n HTTPS Protocol| BB
    AAA -->|Authentication & Authorization \n Business Partner Data, consuming digital services or data assets \n HTTPS Protocol| BBB

    B-->|Authentication & Authorization \n Forwarding Consumer requests like Business Partner Data, \n consuming digital services or data assets \n HTTPS Protocol|CC
    BB-->|Authentication & Authorization \n Forwarding Consumer requests like Business Partner Data, \n consuming digital services or data assets \n HTTPS Protocol|CC
    BBB-->|Authentication & Authorization \n Forwarding Consumer requests like Business Partner Data, \n consuming digital services or data assets \n HTTPS Protocol|CC

    AAAA(Catena-X Members & New C-X Members)
    CCC(CX Portal)
    
    AAAA --->|Upload & create BP, provide unique business partner IDs \n Request for information about Business Partner \n Read only, HTTPS Protocol| CCC

    CC(EDC Operator)
    CCCC(Value Added Services \n example: Country Risk)
    CCCCC(Cleaning Service Providers \n Same Operating Environment)

    D(Gate API)
    DD(Gate Service)
    DDD[(Gate Database)]

    CC-->|Push Business Partner Data \n Request for changed BP \n HTTPS Protocol|D
    CCC-->|Upload and curate Business Partner Data \n HTTPS Protocol|D  
    CCCC-->|Request for specific business partner data \n like specifing sharing member \n HTTPS Protocol|D

    D-->|Forward and Request Business Partner Data|DD
    DD-->|Read & Write Business Partner data & Gate Model Data \n Input Changelog & Output Changelog|DDD

    E(Pool API)
    EE(BPDM Issuer \n BPN Generator)
    EEE(Pool Service)
    EEEE[(Pool Database)]

    CC-->|Consumption of the data \n including metadata ex. legal forms etc. \n Forward data of new Business Partner with \n request for BPN further handled by simulator app \n HTTPS Protocol|E
    CCC-->|Request for information about Business Partner \n Read only HTTPS Protocol|E
    E-->|Request for BPN Creation|EE
    E-->|Forward & Request Golden Record Data|EEE
    EEE-->|Read & Write Golden Record Partner data & Pool Model Data \n Input Changelog & Output Changelog|EEEE


    F(Orchestrator API)

    E<-->|Synchronize Diffrent Gate instances \n and a Poll together to share the data \n between each other over the orchestartor \n asynchronous process HTTPS Protocol|F
    D<-->|Synchronize Diffrent Gate instances \n and a Poll together to share the data \n between each other over the orchestartor \n asynchronous process HTTPS Protocol|F

    CCCCC--->|Cleaning Services including services like natural person screening, \n duplication checks and official registry checks \n HTTPS Protocol|F
    

subgraph Internet Boundary
A
B
AA
BB
AAA
BBB
AAAA

end

subgraph Catena - X Ecosystem
CC
CCC
CCCC
CCCCC

    subgraph BPDM App - K8 Container
         
         
        subgraph Orchestrator Service
         F
         end

         subgraph BPDM Gate
          D
          DD
          DDD
          end

          subgraph BPDM Pool
          E
          EE
          EEE
          EEEE
         end

         
        
    end

    end
```

