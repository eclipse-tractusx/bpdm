# Developer View

Documentation here concerns developers who want to contribute to this repository.

## License Check

Licenses of all maven dependencies need to be approved by eclipse.
The Eclipse Dash License Tool can be used to check the license approval status of dependencies and to request reviews by the intellectual property team.

Generate summary of dependencies and their approval status:

```bash
mvn org.eclipse.dash:license-tool-plugin:license-check -Ddash.summary=DEPENDENCIES
```

Automatically create IP Team review requests:

```bash
mvn org.eclipse.dash:license-tool-plugin:license-check -Ddash.iplab.token=<token>
```

Check the [Eclipse Dash License Tool documentation](https://github.com/eclipse/dash-licenses) for more detailed information.

## Branching Strategy

```mermaid
---
title: BPDM Branching Strategy
---
gitGraph
    commit
    commit
    branch "release/1.0.x"
    checkout "release/1.0.x"
    commit id: "ver(1.0.0)" tag: "1.0.0"

    checkout main
    commit id: "ver(0.1.0-SNAPSHOT)"
    commit id: "fix(A)"
    commit
    checkout "release/1.0.x"
    branch "fix/1.0.x/A"
    cherry-pick id: "fix(A)"
    checkout "release/1.0.x"
    merge "fix/1.0.x/A"

    checkout "release/1.0.x"
    commit id: "ver(1.0.1)" tag: "1.0.1"
    checkout main
    commit
    commit
    branch "release/1.1.x"
    checkout "release/1.1.x"
    commit id: "ver(1.1.0)" tag: "1.1.0"
    checkout main
    commit id: "ver(1.2.0-SNAPSHOT)"
    commit
    commit id: "ver(2.0.0-SNAPSHOT)"
    branch "feat(B)"
    commit id: "feat(B)"
    checkout main
    branch "fix/C"
    commit id: "fix(C)"
```