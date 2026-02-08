#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
#
# SPDX-License-Identifier: GPL-2.0-or-later

set -euo pipefail

test="${1:-}"
if [[ -z "$test" ]]; then
  echo "usage: $0 <fully.qualified.TestClass>" >&2
  echo "example: $0 org.hti5250j.framework.tn5250.InsertCharModePairwiseTest" >&2
  exit 2
fi

chmod +x ./gradlew
./gradlew test --no-daemon --tests "$test"
