{{/*
Copyright (c) 2021,2023 Contributors to the Eclipse Foundation

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
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
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

{{- define "bpdm-bridge.poolServiceName" -}}
{{- $config := .Values.applicationConfig -}}
{{- if and $config (not (empty $config.bpdm)) -}}
    {{- $bpdm := $config.bpdm -}}
    {{- if and $bpdm (not (empty $bpdm.pool)) -}}
        {{- $pool := $bpdm.pool -}}
        {{- if and $pool (not (empty (index $pool "base-url"))) -}}
            {{- index $pool "base-url" -}}
        {{- else -}}
            {{- print "http://" (printf "%s-bpdm-pool" .Release.Name) ":8080" -}}
        {{- end -}}
    {{- else -}}
        {{- print "http://" (printf "%s-bpdm-pool" .Release.Name) ":8080" -}}
    {{- end -}}
{{- else -}}
    {{- print "http://" (printf "%s-bpdm-pool" .Release.Name) ":8080" -}}
{{- end -}}
{{- end }}

{{- define "bpdm-bridge.gateServiceName" -}}
{{- $config := .Values.applicationConfig -}}
{{- if and $config (not (empty $config.bpdm)) -}}
    {{- $bpdm := $config.bpdm -}}
    {{- if and $bpdm (not (empty $bpdm.gate)) -}}
        {{- $gate := $bpdm.gate -}}
        {{- if and $gate (not (empty (index $gate "base-url"))) -}}
            {{- index $gate "base-url" -}}
        {{- else -}}
            {{- print "http://" (printf "%s-bpdm-gate" .Release.Name) ":8080" -}}
        {{- end -}}
    {{- else -}}
        {{- print "http://" (printf "%s-bpdm-gate" .Release.Name) ":8080" -}}
    {{- end -}}
{{- else -}}
    {{- print "http://" (printf "%s-bpdm-gate" .Release.Name) ":8080" -}}
{{- end -}}
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

{/*
Determine postgres service/host name to connect to
*/}}
{{- define "bpdm.postgresDependency" -}}
        {{- include "includeWithPostgresContext" (list $ "postgresql.primary.fullname") }}
{{- end }}}


{{/*
Invoke include on given definition with postgresql dependency context
Usage: include "includeWithPostgresContext" (list $ "your_include_function_here")
*/}}
{{- define "includeWithPostgresContext" -}}
{{- $ := index . 0 }}
{{- $function := index . 1 }}
{{- include $function (dict "Values" $.Values.postgres "Chart" (dict "Name" "postgres") "Release" $.Release) }}
{{- end }}