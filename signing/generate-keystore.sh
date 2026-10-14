#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
keytool -genkeypair -v -keystore release.jks -alias anywhare \
  -keyalg RSA -keysize 4096 -validity 10000
echo
echo "Valor para el secreto KEYSTORE_BASE64:"
base64 -w0 release.jks; echo
