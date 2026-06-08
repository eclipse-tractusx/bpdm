{{/*
Copyright (c) 2021,2024 Contributors to the Eclipse Foundation

See the NOTICE file(s) distributed with this work for additional
information regarding copyright ownership.

This program and the accompanying materials are made available under the
terms of the Apache License, Version 2.0 which is available at
https://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.

SPDX-License-Identifier: Apache-2.0
*/}}

{{/*
Expand the name of the chart.
*/}}
{{- define "bpdm.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "bpdm.fullname" -}}
{{- if .Values.fullnameOverride }}
  {{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else if .Values.nameOverride }}
    {{- include "bpdm.toReleaseName" (list . .Values.nameOverride)  -}}
{{- else }}
    {{- include "bpdm.toReleaseName" (list . .Chart.Name)  -}}
{{- end }}
{{- end }}

{{- define "bpdm.toReleaseName" -}}
{{- $top := first . }}
{{- $name := index . 1 }}
{{- if contains $name $top.Release.Name }}
{{- $top.Release.Name | trunc 63 | trimSuffix "-" }}
{{- else if hasPrefix $top.Release.Name $name  }}
{{- $name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" $top.Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "bpdm.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "bpdm.labels" -}}
helm.sh/chart: {{ include "bpdm.chart" . }}
{{ include "bpdm.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "bpdm.selectorLabels" -}}
app.kubernetes.io/name: {{ include "bpdm.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create name of application secret
*/}}
{{- define "bpdm.applicationSecret.name" -}}
{{- printf "%s-application" (include "bpdm.fullname" .) }}
{{- end }}

{{/*
Resolve the name of a single externally provided application config ConfigMap entry.
Returns `fullname` as-is when set; otherwise calculates the name from the release
name and `name` (like bpdm.fullname). Empty when neither is set.

Usage: include "bpdm.externalApplicationConfig.name" (list $ $entry)
*/}}
{{- define "bpdm.externalApplicationConfig.name" -}}
{{- $ctx := index . 0 -}}
{{- $cfg := index . 1 -}}
{{- if $cfg.fullname -}}
{{- $cfg.fullname | trunc 63 | trimSuffix "-" -}}
{{- else if $cfg.name -}}
{{- include "bpdm.toReleaseName" (list $ctx $cfg.name) -}}
{{- end -}}
{{- end -}}

{{/*
Resolve the determined full name of a dependency (sub)chart from the umbrella context.
Replicates the standard Helm fullname algorithm (fullnameOverride wins; otherwise the
release name combined with nameOverride|chartName, collapsed when the release name already
contains it). Lets the umbrella compute e.g. the deployed Postgres/Keycloak service name
from the values it passes to those dependencies.

Usage: include "bpdm.dependencyFullname" (list $ $dependencyValues "chartName")
*/}}
{{- define "bpdm.dependencyFullname" -}}
{{- $ctx := index . 0 -}}
{{- $values := index . 1 | default dict -}}
{{- $chartName := index . 2 -}}
{{- if $values.fullnameOverride -}}
{{- $values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := $values.nameOverride | default $chartName -}}
{{- if contains $name $ctx.Release.Name -}}
{{- $ctx.Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" $ctx.Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Resolve the ordered list of externally provided application config ConfigMap names.
Iterates `.Values.externalApplicationConfig` (a list of {name|fullname} entries),
preserving order and dropping entries that resolve to an empty name.
*/}}
{{- define "bpdm.externalApplicationConfig.names" -}}
{{- $ctx := . -}}
{{- $names := list -}}
{{- range $ctx.Values.externalApplicationConfig | default list -}}
{{- $name := include "bpdm.externalApplicationConfig.name" (list $ctx .) -}}
{{- if $name -}}{{- $names = append $names $name -}}{{- end -}}
{{- end -}}
{{- $names | toJson -}}
{{- end -}}

{{- /*
Merges three templates one after another in the following order:
valuesOverride -overrides-> (defaultOverride -overrides-> baseTemplate)

Usage: include "bpdm-common.threeWayMerge" ("context" $ "baseTemplate" "templateName" "defaultOverride" "templateName" "valuesOverride" "templateName")
*/}}
{{- define "bpdm-common.threeWayMerge" -}}
    {{- if .defaultOverride -}}
        {{- $ := .context -}}
        {{- $baseTemplateDict := fromYaml (include .baseTemplate $) | default (dict ) -}}
        {{- $defaultOverrideDict := fromYaml (include .defaultOverride $) | default (dict )  -}}
        {{- $valuesOverrideDict:= fromYaml (include .valuesOverride $) | default (dict )  -}}
        {{- $finalDict :=  merge $valuesOverrideDict $defaultOverrideDict $baseTemplateDict -}}
        {{- toYaml $finalDict -}}
    {{- else -}}
        {{- include "bpdm-common.merge" (dict "context" .context "baseTemplate" .baseTemplate "override" .valuesOverride) }}
    {{- end -}}
{{- end -}}

{{- define "bpdm-common.merge" -}}
    {{- $ := .context -}}
    {{- $baseTemplateDict := fromYaml (include .baseTemplate $) | default (dict ) -}}
    {{- $overrideDict := fromYaml (include .override $) | default (dict )  -}}
    {{- $finalDict :=  merge $overrideDict $baseTemplateDict -}}
    {{- toYaml $finalDict -}}
{{- end -}}

{{- define "bpdm-common.empty" -}}
{{- end -}}