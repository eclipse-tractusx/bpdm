# Runtime View

## Upsert Generic Business Partner

> [!NOTE]
> An additional endpoint was implemented as requirements came up that required business partner data records not to be fed directly into the golden record process after an upload. Instead, this endpoint makes it possible to change the status of a business partner data record from "inital" to "ready". Only data records with the status "ready" are fed into the golden record process.
> We are aware that the existing integration scenarios, such as with the portal team, are impacted by this. For this reason, we recommend that the gate is configured accordingly so that the status is set to "ready" by default when a data record is uploaded. The operator can configure this behavior in the gate individually based on the requirements.

```mermaid
sequenceDiagram
    autonumber

    SharingMember->>Gate: PUT api/catena/input/business-partners <br> Payload: Business Partner Data A
    Gate-->>Gate: Persist Business Partner Data Input
    Gate-->>Gate: Set Sharing State to 'Initial'
    Gate-->>Gate: Add Changelog Entry 'Create' for Business Partner Input
    Gate-->>SharingMember: Upserted Business Partner

    SharingMember->>Gate: POST api/catena/sharing-state/ready <br> Payload: External ID A
    Gate-->>Gate: Set Sharing State to 'Ready'
    Gate-->>SharingMember: OK

    loop Polling for Ready Business Partners
        Gate-->>Gate: Fetch Business Partners in State 'Ready'
        Gate->>Orchestrator: POST api/golden-record-tasks <br> Payload: Business Partner Input Data in mode 'UpdateFromSharingMember'
        Orchestrator-->>Orchestrator: Create Golden Record Task for Business Partner Data
        Orchestrator-->>Orchestrator: Set Golden Record Task State <br> Result State: 'Pending'
        Orchestrator-->>Orchestrator: Set Golden Record Task State <br> Step: 'CleanAndSync' <br> StepState: 'Queued'
        Orchestrator-->>Gate: Created Golden Record Task
        Gate-->>Gate: Set Sharing State <br> Type: 'PENDING' <br> Task ID: Golden Record Task ID
    end

    loop Polling for Step 'CleanAndSync'
        CleaningServiceDummy->>Orchestrator: POST api/golden-record-tasks/step-reservations <br> Payload: Step 'CleanAndSync'
        Orchestrator-->>Orchestrator: Set Golden Record Task State <br> Step: 'CleanAndSync' <br> StepState: 'Reserved'
        Orchestrator-->>CleaningServiceDummy: Golden Record Task
        CleaningServiceDummy-->>CleaningServiceDummy: Set L/S/A and Generic Business Partner Dummy Cleaning Result
        CleaningServiceDummy-->>CleaningServiceDummy: Set BPN References to L/S/A result
        CleaningServiceDummy->>Orchestrator: POST api/golden-record-tasks/step-results <br> Payload: Dummy Result
        Orchestrator-->>Orchestrator: Set Golden Record Task Business Partner Data to Dummy Result
        Orchestrator-->>Orchestrator: Set Golden Record Task State <br> Step: 'PoolSync' <br> StepState: 'Queued'
        Orchestrator-->>CleaningServiceDummy: Accept
    end

    loop Polling for Step 'PoolSync'
        Pool->>Orchestrator: POST api/golden-record-tasks/step-reservations <br> Payload: Step 'PoolSync'
        Orchestrator-->>Orchestrator: Set Golden Record Task State <br> Step: 'PoolSync' <br> StepState: 'Reserved'
        Orchestrator-->>Pool: Golden Record Task
        opt Golden Record Legal Entity Data marked as changed
            Pool-->>Pool: Upsert Legal Entity from Golden Record Task Legal Entity Data
            Pool-->>Pool: Add Changelog Entry for BPNL
        end
        opt Golden Record Site Data marked as changed
            Pool-->>Pool: Upsert Site from Golden Record Task Site Data
            Pool-->>Pool: Add Changelog Entry for BPNS
        end
         opt Golden Record Address Data marked as changed
            Pool-->>Pool: Upsert Address from Golden Record Task Address Data
             Pool-->>Pool: Add Changelog Entry for BPNA
        end
        Pool-->>Pool: Set BPNs in Golden Record Task Generic Business Partner Data
        Pool->>Orchestrator: POST api/golden-record-tasks/step-results <br> Payload: Updated Result
        Orchestrator-->>Pool: Accept
        Orchestrator-->>Orchestrator: Set Golden Record Task State <br> Step: 'PoolSync' <br> Step State: 'Success'
         Orchestrator-->>Orchestrator: Set Golden Record Task State <br> Result State: 'Success'
    end
    
    loop Polling for finished Golden Record Tasks
        Gate-->>Gate: Query sharing states in Sharing State Type 'PENDING'
        Gate->>Orchestrator: POST golden-record-tasks/state/search <br> Payload: Golde Record Task ID
        Orchestrator-->Gate: Golden Record Task State and Result
        Gate-->>Gate: Persist Business Partner Output
        Gate-->>Gate: Set Sharing State 'Success'
        Gate-->>Gate: Add Changelog Entry 'Create' for Business Partner Output
    end

    SharingMember->>Gate: POST api/catena/output/changelog/search <br> Payload: From After Last Search Time
    Gate-->>SharingMember: Changelog entry with Business Partner External ID
    SharingMember->>Gate: POST api/catena/output/business-partners/search <br> Payload: External ID
    Gate-->>SharingMember: Business Partner Output
```

## Update on Golden Record Change

```mermaid
sequenceDiagram
    autonumber

    Pool-->Pool: Add Changelog Entry for BPNL 
    
    loop Polling Pool Changelog
        Gate->>Pool: POST api/catena/changelog/search <br> Payload: From After Last Search Time
        Pool-->>Gate: Changelog entry for BPNL
        Gate-->>Gate: Query Business Partner Output with BPNL
        Gate->>Orchestrator: POST api/golden-record-tasks <br> Payload: Business Partner Output Data in mode 'UpdateFromPool'
        Orchestrator-->>Orchestrator: Create Golden Record Task for Business Partner Data
        Orchestrator-->>Orchestrator: Set Golden Record Task State <br> Result State: 'Pending'
        Orchestrator-->>Orchestrator: Set Golden Record Task State <br> Step: 'Clean' <br> StepState: 'Queued'
        Orchestrator-->>Gate: Created Golden Record Task
        Gate-->>Gate: Set Sharing State <br> Type: 'PENDING' <br> Task ID: Golden Record Task ID
    end

    loop Polling for Step 'Clean'
        CleaningServiceDummy->>Orchestrator: POST api/golden-record-tasks/step-reservations <br> Payload: Step 'Clean'
        Orchestrator-->>Orchestrator: Set Golden Record Task State <br> Step: 'CleanAndSync' <br> StepState: 'Reserved'
        Orchestrator-->>CleaningServiceDummy: Golden Record Task
        CleaningServiceDummy->>Orchestrator: POST api/golden-record-tasks/step-results <br> Payload: Golden Record Task Business Partner Data
        Orchestrator-->>Orchestrator: Set Golden Record Task Business Partner Data to Dummy Result
        Orchestrator-->>Orchestrator: Set Golden Record Task State <br> Step: 'Clean' <br> Step State: 'Success'
         Orchestrator-->>Orchestrator: Set Golden Record Task State <br> Result State: 'Success'
        Orchestrator-->>CleaningServiceDummy: Accept
    end
    
    loop Polling for finished Golden Record Tasks
        Gate-->>Gate: Query sharing states in Sharing State Type 'PENDING'
        Gate->>Orchestrator: POST golden-record-tasks/state/search <br> Payload: Golden Record Task ID
        Orchestrator-->Gate: Golden Record Task State and Result
        Gate-->>Gate: Persist Business Partner Output
        Gate-->>Gate: Set Sharing State 'Success'
        Gate-->>Gate: Add Changelog Entry 'Create' for Business Partner Output
    end
    
    SharingMember->>Gate: POST api/catena/output/changelog/search <br> Payload: From After Last Search Time
    Gate-->>SharingMember: Changelog entry with Business Partner External ID
    SharingMember->>Gate: POST api/catena/output/business-partners/search <br> Payload: External ID
    Gate-->>SharingMember: Business Partner Output
    
```

## Business Partner Data Records - States

This sections describes the different states a business partner data record can have.

### Automatically executing golden record process

```mermaid
---
title: state diagram business partner for automatically executing golden record process
---
stateDiagram-v2
    [*] --> ready: sharing member uploads bp into gate
    note right of ready
      Gate is configured to automatically <br> set state to ready after bp upload
    end note
    ready --> pending: scheduler initiates <br> the golden record process
    state if_state <<choice>>
    pending --> if_state: run golden record process
    if_state --> success: if golden record process succeeded
    if_state --> error: if golden record process failed
```

### Manually triggering golden record process

```mermaid
---
title: state diagram business partner for manual golden record process triggering
---
stateDiagram-v2
    [*] --> initial: sharing member uploads bp into gate
    note right of initial
      POST api/catena/sharing-state/ready <br> Payload: External ID A
    end note
    initial --> ready: sharing member or third-party <br> service calls separate endpoint
    ready --> pending: scheduler initiates <br> the golden record process
    state if_state <<choice>>
    pending --> if_state: run golden record process
    if_state --> success: if golden record process succeeded
    if_state --> error: if golden record process failed
```


## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2023,2024 ZF Friedrichshafen AG
- SPDX-FileCopyrightText: 2023,2024 SAP SE
- SPDX-FileCopyrightText: 2023,2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
- SPDX-FileCopyrightText: 2023,2024 Mercedes Benz Group
- SPDX-FileCopyrightText: 2023,2024 Robert Bosch GmbH
- SPDX-FileCopyrightText: 2023,2024 Schaeffler AG
- SPDX-FileCopyrightText: 2023,2024 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/bpdm