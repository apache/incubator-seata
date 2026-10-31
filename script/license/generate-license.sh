#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# ---------------------------------------------------------------------------
# generate-license.sh
#
# Shell wrapper around generate-license.py.
# Ensures the correct working directory and Python environment.
#
# Usage:
#   ./script/license/generate-license.sh [namingserver|server|distribution|all]
# ---------------------------------------------------------------------------

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"

cd "$PROJECT_ROOT"

MODULE="${1:-all}"

# Check that license-eye is available
LICENSE_EYE=""
if command -v license-eye &>/dev/null; then
    LICENSE_EYE="license-eye"
elif [ -x "$HOME/go/bin/license-eye" ]; then
    LICENSE_EYE="$HOME/go/bin/license-eye"
else
    echo "ERROR: license-eye not found."
    echo "Install with:"
    echo "  brew install license-eye"
    echo "  go install github.com/apache/skywalking-eyes/cmd/license-eye@latest"
    exit 1
fi

# Ensure license-eye is in PATH during Python execution
export PATH="$HOME/go/bin:$PATH"

echo "Using license-eye: $(which license-eye 2>/dev/null || echo "$LICENSE_EYE")"
echo "Generating LICENSE files for: $MODULE"
echo ""

python3 script/license/generate-license.py "$@"
