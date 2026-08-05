<!--
Use this template to change the project version: either cutting a new release
candidate or returning to a development snapshot. Both directions touch the same
files — only the target version string differs.

How to use it:
  Append `?template=version_bump.md` to the PR "compare" URL, e.g.
  https://github.com/eclipse-tractusx/bpdm/compare/main...my-branch?template=version_bump.md

Pick your scenario below, set <TARGET_VERSION> accordingly, then work the checklist.
Everything past the scenario section refers to that single <TARGET_VERSION>.
-->

# Build: `<TARGET_VERSION>` SNAPSHOT/Release Candidate

## Scenario

Tick the direction this PR takes and fill in the target version:

- [ ] **Create release candidate** — set `<TARGET_VERSION>` to `<VERSION>-rc<X>`
  (`<VERSION>` = current version, e.g. `7.4.0`; `<X>` = the RC count for this version, e.g. `1` for the first → `7.4.0-rc1`)
- [ ] **Return to snapshot** — set `<TARGET_VERSION>` to `<VERSION>-SNAPSHOT`
  (`<VERSION>` = the development version, e.g. `7.4.0` to resume the same line, or the next version after a release)

## Checklist

- [ ] **pom.xml** — set the `<revision>` property to `<TARGET_VERSION>`, i.e. `<revision><TARGET_VERSION></revision>` (`pom.xml`, the `<revision>` property)
- [ ] **Helm charts — application versions** — set `appVersion` to `"<TARGET_VERSION>"` in every chart:
  - [ ] `charts/bpdm/Chart.yaml`
  - [ ] `charts/bpdm/charts/bpdm-pool/Chart.yaml`
  - [ ] `charts/bpdm/charts/bpdm-gate/Chart.yaml`
  - [ ] `charts/bpdm/charts/bpdm-orchestrator/Chart.yaml`
  - [ ] `charts/bpdm/charts/bpdm-cleaning-service-dummy/Chart.yaml`
- [ ] **Helm charts — chart versions** — set the `version` field suffix to match the target (`-rc<X>` or `-SNAPSHOT`) for every chart that is part of this release (umbrella `bpdm` and the `bpdm-pool`, `bpdm-gate`, `bpdm-orchestrator`, `bpdm-cleaning-service-dummy` subcharts)
- [ ] **Helm charts — dependency versions** — in `charts/bpdm/Chart.yaml`, adapt each `dependencies[].version` so it matches the updated subchart `version` values above (do **not** touch external deps such as `bpdm-common`, `postgres`, `keycloak`)
- [ ] **Chart READMEs** — regenerate with helm-docs so the README versions match the updated `Chart.yaml` files:
  ```bash
  helm-docs --chart-search-root=charts
  ```
  (uses helm-docs v1.14.2)
- [ ] **OpenAPI docs** — update the `info.version` to `<TARGET_VERSION>` in every API document under `docs/api/`:
  - [ ] `docs/api/pool.yaml` / `docs/api/pool.json`
  - [ ] `docs/api/gate.yaml` / `docs/api/gate.json`
  - [ ] `docs/api/orchestrator.yaml` / `docs/api/orchestrator.json`

## Verification

- [ ] `mvn clean install -DskipTests` succeeds with the new revision
- [ ] `helm dependency build charts/bpdm` resolves all subchart dependency versions
- [ ] No stray version references remain — the only version suffix present should be the intended one, with no leftover suffix from the previous version:
  ```bash
  grep -rn "SNAPSHOT\|-rc" pom.xml charts docs/api
  ```
