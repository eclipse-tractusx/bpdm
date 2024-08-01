---
################################################################################
# Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
#
# See the NOTICE file(s) distributed with this work for additional
# information regarding copyright ownership.
#
# This program and the accompanying materials are made available under the
# terms of the Apache License, Version 2.0 which is available at
# https://www.apache.org/licenses/LICENSE-2.0.
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.
#
# SPDX-License-Identifier: Apache-2.0
################################################################################

{{- define "bpdm-common.deployment" -}}
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "bpdm.fullname" . }}
  labels:
    {{- include "bpdm.labels" . | nindent 4 }}
spec:
  {{- if not .Values.autoscaling.enabled }}
  replicas: {{ .Values.replicaCount }}
  {{- end }}
  selector:
    matchLabels:
      {{- include "bpdm.selectorLabels" . | nindent 6 }}
  template:
    metadata:
      annotations:
        checksum/config: {{ include (print .Template.BasePath "/configMap.yaml") . | sha256sum }}
      {{- with .Values.podAnnotations}}
      {{- toYaml . | nindent 8}}
      {{- end}}
      labels:
        {{- include "bpdm.selectorLabels" . | nindent 8}}
    spec:
      {{- with .Values.imagePullSecrets }}
      imagePullSecrets:
        {{- toYaml . | nindent 8 }}
      {{- end }}
      # @url: https://kubernetes.io/docs/tasks/configure-pod-container/configure-service-account/#use-the-default-service-account-to-access-the-api-server
      automountServiceAccountToken: false
      securityContext:
        {{- toYaml .Values.podSecurityContext | nindent 8 }}
      containers:
        - name: {{ .Chart.Name }}
          securityContext:
            {{- toYaml .Values.securityContext | nindent 12 }}
          image: "{{ .Values.image.registry }}/{{ .Values.image.repository }}:{{ .Values.image.tag | default .Chart.AppVersion }}"
          imagePullPolicy: {{ .Values.image.pullPolicy }}
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: {{ .Values.springProfiles | join ","  }}
            - name: SPRING_CONFIG_IMPORT
              value: "/etc/conf/deployment.yml,/etc/conf/secrets.yml"
          ports:
            - name: http
              containerPort: {{ .Values.service.targetPort }}
              protocol: TCP
          # @url: https://cloud.google.com/blog/products/containers-kubernetes/kubernetes-best-practices-setting-up-health-checks-with-readiness-and-liveness-probes
          livenessProbe:
            {{- toYaml .Values.livenessProbe | nindent 12 }}
          readinessProbe:
            {{- toYaml .Values.readinessProbe | nindent 12 }}
          startupProbe:
            {{- toYaml .Values.startupProbe | nindent 12 }}
          resources:
            {{- toYaml $.Values.resources | nindent 12 }}
          volumeMounts:
            - mountPath: /etc/conf
              name: config
              readOnly: true
            - mountPath: /tmp
              name: cache
      initContainers:
        - name: startup-delay
          image: busybox:1.28
          command: ['sh', '-c', "sleep {{ $.Values.startupDelaySeconds }}"]
      {{- with .Values.nodeSelector }}
      nodeSelector:
        {{- toYaml . | nindent 8 }}
      {{- end }}
      {{- with .Values.affinity }}
      affinity:
        {{- toYaml . | nindent 8 }}
      {{- end }}
      {{- with .Values.tolerations }}
      tolerations:
        {{- toYaml . | nindent 8 }}
      {{- end }}
      volumes:
        - name: config
          projected:
            sources:
              - configMap:
                  name: {{ include "bpdm.fullname" . }}
              - secret:
                  name: {{ include "bpdm.fullname" . }}
        - name: cache
          emptyDir:
            sizeLimit: 200Mi
{{- end -}}