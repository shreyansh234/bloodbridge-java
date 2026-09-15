#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if [[ ! -f .env ]]; then python3 setup-admin.py; fi
exec mvn spring-boot:run -Dspring-boot.run.profiles=demo
