<!--
Use this template when a release has just been cut and the development line
should move on to the next SNAPSHOT version.

This is the follow-up to the snapshot <-> release-candidate switch
(`snapshot-release-candidate-switch.md`): once `<VERSION>` is released, this PR
opens the next development cycle for `<NEXT_VERSION>-SNAPSHOT`.

How to use it:
  Append `?template=release-to-snapshot-switch.md` to the PR "compare" URL, e.g.
  https://github.com/eclipse-tractusx/bpdm/compare/main...my-branch?template=release-to-snapshot-switch.md

Fill in the version placeholders below, then work the checklist top to bottom.
-->

# Build: post-release switch to `<NEXT_VERSION>-SNAPSHOT`

## Versions

Fill in the three values this PR revolves around:

- **`<RELEASED_VERSION>`** — the version that was just released (e.g. `7.4.0`)
- **`<RELEASE_BRANCH>`** — its release branch `release/<MAJOR>.<MINOR>.x` (e.g. `release/7.4.x`)
- **`<TARGET_VERSION>`** — the next development version `<NEXT_VERSION>-SNAPSHOT`
  (`<NEXT_VERSION>` = the next minor after the released one, e.g. `7.5.0` → `7.5.0-SNAPSHOT`)

## 1. Preserve the released version on a release branch

- [ ] **Release branch** — create `<RELEASE_BRANCH>` from the released commit (the `<RELEASED_VERSION>` build on `main`) and push it
- [ ] **`.tractusx`** — register the new release branch's three API specs under `openApiSpecs`, inserted **above** the existing `main` entries (keep the ascending branch order):
  ```yaml
  - "https://raw.githubusercontent.com/eclipse-tractusx/bpdm/refs/heads/<RELEASE_BRANCH>/docs/api/gate.yaml"
  - "https://raw.githubusercontent.com/eclipse-tractusx/bpdm/refs/heads/<RELEASE_BRANCH>/docs/api/orchestrator.yaml"
  - "https://raw.githubusercontent.com/eclipse-tractusx/bpdm/refs/heads/<RELEASE_BRANCH>/docs/api/pool.yaml"
  ```

## 2. Bump the development line to `<TARGET_VERSION>`

- [ ] **pom.xml** — set the `<revision>` property to `<TARGET_VERSION>`, i.e. `<revision><TARGET_VERSION></revision>` (`pom.xml`, the `<revision>` property)
- [ ] **Helm charts — application versions** — set `appVersion` to `"<TARGET_VERSION>"` in every chart:
  - [ ] `charts/bpdm/Chart.yaml`
  - [ ] `charts/bpdm/charts/bpdm-pool/Chart.yaml`
  - [ ] `charts/bpdm/charts/bpdm-gate/Chart.yaml`
  - [ ] `charts/bpdm/charts/bpdm-orchestrator/Chart.yaml`
  - [ ] `charts/bpdm/charts/bpdm-cleaning-service-dummy/Chart.yaml`
- [ ] **Helm charts — chart versions** — for every chart that is part of this release (umbrella `bpdm` and the `bpdm-pool`, `bpdm-gate`, `bpdm-orchestrator`, `bpdm-cleaning-service-dummy` subcharts), bump the `version` to the next anticipated chart version **and** append the `-SNAPSHOT` suffix (e.g. umbrella `7.0.0` → `7.1.0-SNAPSHOT`). Each chart has its own independent semver — bump per chart, not all to the same number.
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

## 3. Open the documentation for the new cycle

- [ ] **App changelog** — in `CHANGELOG.md`, add a new top section for the upcoming version with empty subsections, ready to fill during the cycle:
  ```markdown
  ## [<NEXT_VERSION>] - unreleased

  ### Breaking

  ### Added

  ### Changed
  ```
- [ ] **Charts changelog** — in `charts/bpdm/CHANGELOG.md`, add a new top section headed by the new **umbrella chart** `version` (the bumped value, without the `-SNAPSHOT` suffix) with empty subsections:
  ```markdown
  ## [<NEXT_UMBRELLA_CHART_VERSION>] - unreleased

  ### Breaking

  ### Added

  ### Changed
  ```
- [ ] **Migration guide** — in `docs/admin/MIGRATION_GUIDE.md`, add a new empty section for the upcoming version above the most recent one, and add a matching entry to the table of contents:
  ```markdown
  ## <RELEASED_MAJOR.MINOR>.x to <NEXT_MAJOR.MINOR>.x

  _No migration steps yet._
  ```
  (e.g. `## 7.4.x to 7.5.x` when moving from `7.4.0` to `7.5.0-SNAPSHOT`)

## Verification

- [ ] `mvn clean install -DskipTests` succeeds with the new revision
- [ ] `helm dependency build charts/bpdm` resolves all subchart dependency versions
- [ ] The new release branch's API specs are reachable (the `<RELEASE_BRANCH>` URLs in `.tractusx` resolve once the branch is pushed)
- [ ] No stray version references remain — every version suffix present should be `-SNAPSHOT`, with no leftover `-rc` or bare release version from the previous build:
  ```bash
  grep -rn "SNAPSHOT\|-rc" pom.xml charts docs/api
  ```
