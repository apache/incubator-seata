#!/usr/bin/env python3
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
"""
Auto-generate LICENSE-namingserver and LICENSE-server files.

Uses Apache SkyWalking Eyes (license-eye) to resolve transitive Maven and npm
dependencies per module, then formats the output into the ASF-required LICENSE
file format.

Usage:
    python3 script/license/generate-license.py namingserver
    python3 script/license/generate-license.py server
    python3 script/license/generate-license.py all

Prerequisites:
    - license-eye must be installed and in PATH
      brew install license-eye
      go install github.com/apache/skywalking-eyes/cmd/license-eye@latest
    - Maven local repository must have all dependencies downloaded
      mvn install -DskipTests
"""

import argparse
import os
import re
import shutil
import subprocess
import sys
import tempfile
from collections import defaultdict
from pathlib import Path


# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent
DIST_DIR = PROJECT_ROOT / "distribution"
LICENSES_DIR = DIST_DIR / "licenses"

MODULE_CONFIG = {
    "namingserver": {
        "config_file": PROJECT_ROOT / ".licenserc-namingserver.yaml",
        "output_file": DIST_DIR / "LICENSE-namingserver",
        "maven_module": "namingserver",
    },
    "server": {
        "config_file": PROJECT_ROOT / ".licenserc-server.yaml",
        "output_file": DIST_DIR / "LICENSE-server",
        "maven_module": "server",
    },
    "distribution": {
        "config_file": PROJECT_ROOT / ".licenserc-distribution.yaml",
        "output_file": DIST_DIR / "LICENSE",
        "maven_module": "distribution",
    },
}

# Extra entries for distribution/LICENSE that license-eye cannot auto-resolve.
# These are font files bundled with the console webapp, dual-licensed
# dependencies, and other manually verified entries.
DISTRIBUTION_EXTRA_ENTRIES = """
========================================================================
Apache-2.0 licenses (additional)
========================================================================

    The following font files are included in the Seata Console:
    console/src/main/resources/static/console-fe/public/css/fonts/

    roboto-regular.ttf Apache-2.0
    roboto-regular.woff Apache-2.0
    roboto-regular.woff2 Apache-2.0
    roboto-bold.ttf Apache-2.0
    roboto-bold.woff Apache-2.0
    roboto-bold.woff2 Apache-2.0
    (Google Roboto Font - https://fonts.google.com/specimen/Roboto)

========================================================================
MIT licenses (additional)
========================================================================

    The following font files are bundled with @alicloud/console-components (MIT):
    console/src/main/resources/static/console-fe/public/css/fonts/

    aliyun-console-font.eot MIT (bundled with @alicloud/console-components)
    aliyun-console-font.ttf MIT (bundled with @alicloud/console-components)
    aliyun-console-font.woff MIT (bundled with @alicloud/console-components)

    The following icon font files are from iconfont.cn (Alibaba):
    font_515771_emcns5054x3whfr.ttf MIT (iconfont.cn - https://www.iconfont.cn)
    font_515771_emcns5054x3whfr.woff MIT (iconfont.cn - https://www.iconfont.cn)

========================================================================
CDDL+GPL-1.1 licenses
========================================================================

    javax.servlet:javax.servlet-api 4.0.1 CDDL+GPL-1.1 see:licenses/CDDL+GPL-1.1
    org.glassfish:javax.json 1.0.4 CDDL+GPL-1.1 see:licenses/CDDL+GPL-1.1
    com.sun.jersey.contribs:jersey-apache-client4 1.19.1 CDDL+GPL-1.1 see:licenses/CDDL+GPL-1.1
    com.sun.jersey:jersey-client 1.19.1 CDDL+GPL-1.1 see:licenses/CDDL+GPL-1.1
    com.sun.jersey:jersey-core 1.19.1 CDDL+GPL-1.1 see:licenses/CDDL+GPL-1.1
"""

# Known license overrides for dependencies whose license cannot be
# auto-resolved by license-eye.  Map: dependency_name -> license_spdx
LICENSE_OVERRIDES = {
    "com.fasterxml.jackson.dataformat:jackson-dataformat-yaml": "Apache-2.0",
    "com.fasterxml.jackson.datatype:jackson-datatype-jsr310": "Apache-2.0",
    "com.fasterxml.jackson.module:jackson-module-jsonSchema": "Apache-2.0",
    "com.github.victools:jsonschema-generator": "Apache-2.0",
    "com.github.victools:jsonschema-module-jackson": "Apache-2.0",
    "com.github.victools:jsonschema-module-swagger-2": "Apache-2.0",
    "org.antlr:antlr-runtime": "BSD",
    "com.alipay.sofa:jraft-core": "Apache-2.0",
    "com.github.andrewoma.dexx:dexx-collections": "MIT",
    "com.thoughtworks.xstream:xstream": "BSD-3-Clause",
    "org.apache.ant:ant": "Apache-2.0",
    "org.apache.ant:ant-launcher": "Apache-2.0",
    "org.apache.zookeeper:zookeeper": "Apache-2.0",
    "org.apache.zookeeper:zookeeper-jute": "Apache-2.0",
    "io.github.x-stream:mxparser": "BSD-3-Clause",
    "net.java.dev.jna:jna": "LGPL-2.1",
    "org.json:json": "Public-Domain",
    "org.reflections:reflections": "WTFPL",
    "org.javassist:javassist": "MPL-1.1",
}

# License normalization: various spellings → SPDX identifier
LICENSE_NORMALIZE = {
    # Text variants
    "Public Domain": "Public-Domain",

    # ANTLR licenses (BSD)
    "http://www.antlr.org/license.html": "BSD",
    "https://www.antlr.org/license.html": "BSD",

    # SPDX URL variants → SPDX identifier
    "https://spdx.org/licenses/BSD-3-Clause.html": "BSD-3-Clause",
    "https://spdx.org/licenses/MIT-0.html": "MIT-0",

    # Apache License URL variants
    "http://www.apache.org/licenses/LICENSE-1.1.txt": "Apache-1.1",
    "http://www.apache.org/licenses/LICENSE-2.0.html": "Apache-2.0",
    "https://www.apache.org/licenses/LICENSE-2.0.html": "Apache-2.0",

    # CDDL URL variants
    "http://www.opensource.org/licenses/cddl1.php": "CDDL-1.0",

    # BSD URL variants
    "https://opensource.org/licenses/BSD-2-Clause;description=BSD 2-Clause License": "BSD-2-Clause",

    # PostgreSQL JDBC
    "https://jdbc.postgresql.org/about/license.html": "BSD-2-Clause",

    # logback dual license (EPL-2.0 / LGPL-2.1, take first for ASF convention)
    "https://www.eclipse.org/legal/epl-v20.html, https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html": "EPL-2.0",

    # Jersey dual license (CDDL / GPL, take first for ASF convention)
    "http://glassfish.java.net/public/CDDL+GPL_1_1.html, http://glassfish.java.net/public/CDDL+GPL_1_1.html": "CDDL-1.0",
    "https://glassfish.java.net/public/CDDL+GPL_1_1.html, https://glassfish.java.net/public/CDDL+GPL_1_1.html": "CDDL-1.0",

    "https://github.com/redis/redis-authx-core/blob/master/LICENSE": "MIT",
    "https://github.com/redis/jedis/blob/master/LICENSE": "MIT",

    "https://github.com/stleary/JSON-java/blob/master/LICENSE": "Public-Domain",

    # mxparser (BSD-3-Clause)
    "https://raw.githubusercontent.com/x-stream/mxparser/master/LICENSE.txt": "BSD-3-Clause",

    # JNA (LGPL-2.1)
    "http://www.gnu.org/licenses/licenses.html": "LGPL-2.1",

    # Reflections (WTFPL / BSD dual license, take first for ASF convention)
    "http://www.wtfpl.net/, http://www.opensource.org/licenses/bsd-license.html": "WTFPL",
}

# License sections ordering (first = appears first in the file)
LICENSE_ORDER = [
    "0BSD",
    "Apache-1.1",
    "Apache-2.0",
    "BSD",
    "BSD-2-Clause",
    "BSD-3-Clause",
    "CC0-1.0",
    "CC-BY-4.0",
    "CDDL-1.0",
    "EPL-1.0",
    "EPL-2.0",
    "GPL-2.0-with-classpath-exception",
    "ISC",
    "LGPL-2.1",
    "MIT",
    "MIT-0",
    "MPL-1.1",
    "MPL-2.0",
    "Public-Domain",
    "SIL",
    "Unlicense",
    "WTFPL",
]

def read_apache_license_header() -> str:
    """Read the Apache License header from the project root LICENSE file.

    Extracts only the Apache License text (before the subcomponents section
    which is delimited by "========").
    """
    license_file = PROJECT_ROOT / "LICENSE"
    if not license_file.exists():
        print(f"ERROR: LICENSE file not found: {license_file}")
        sys.exit(1)

    content = license_file.read_text(encoding="utf-8")

    # The LICENSE file contains the Apache License text followed by
    # subcomponent entries. Split on the first "========" separator
    # to extract just the Apache License portion.
    separator = "\n   ======================================================================="
    if separator in content:
        return content.split(separator)[0]

    return content.rstrip()


# Cached at module load time
APACHE_LICENSE_HEADER = read_apache_license_header()

SEATA_SUBCOMPONENTS_HEADER = """=======================================================================
Seata Subcomponents:

The Seata project contains subcomponents with separate copyright
notices and license terms. Your use of the source code for the these
subcomponents is subject to the terms and conditions of the following
licenses.
"""


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def find_license_eye():
    """Find the license-eye binary."""
    path = shutil.which("license-eye")
    if path:
        return path
    # Also check GOPATH
    gopath = os.environ.get("GOPATH", os.path.expanduser("~/go"))
    candidate = os.path.join(gopath, "bin", "license-eye")
    if os.path.isfile(candidate):
        return candidate
    return None


def resolve_dependencies(config_file: Path) -> list[tuple[str, str, str]]:
    """
    Run license-eye dependency resolve and return list of
    (dependency_name, license_spdx, version) tuples.
    """
    license_eye = find_license_eye()
    if not license_eye:
        print("ERROR: license-eye not found in PATH or ~/go/bin.")
        print("Install with: brew install license-eye")
        print("          or: go install github.com/apache/skywalking-eyes/cmd/license-eye@latest")
        sys.exit(1)

    with tempfile.TemporaryDirectory(prefix="license-gen-") as tmpdir:
        cmd = [
            license_eye, "dependency", "resolve",
            "-c", str(config_file),
            "-o", tmpdir,
            "-v", "info",
        ]
        print(f"  Running: {' '.join(cmd)}")
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            cwd=str(PROJECT_ROOT),
            timeout=300,
        )

        # license-eye outputs the dependency table on stdout (after log lines)
        output = result.stdout

        deps = []
        in_table = False
        for line in output.splitlines():
            # Strip ANSI escape codes for parsing
            line_clean = re.sub(r'\x1b\[[0-9;]*m', '', line).strip()
            if not line_clean:
                continue

            # Detect table header
            if "Dependency" in line_clean and "License" in line_clean and "Version" in line_clean:
                in_table = True
                continue

            if not in_table:
                continue

            # Skip separator line (----)
            if line_clean.startswith("---"):
                continue

            # Stop at ERROR line
            if line_clean.startswith("ERROR"):
                break

            # Parse table row: name | license | version
            parts = [p.strip() for p in line_clean.split("|")]
            if len(parts) >= 3:
                dep_name = parts[0]
                license_raw = parts[1]
                version = parts[2]
                if dep_name and version:
                    deps.append((dep_name, license_raw, version))

        return deps


def normalize_license(license_raw: str) -> str:
    """Normalize a license string to SPDX identifier."""
    license_raw = license_raw.strip()
    if license_raw == "Unknown" or not license_raw:
        return "Unknown"

    # Handle multi-licensing: "EPL-1.0 and LGPL-2.1" -> take first license
    # (matches ASF convention for logback, tomcat, spring, etc.)
    if " and " in license_raw.lower():
        first = re.split(r'\s+and\s+', license_raw, flags=re.IGNORECASE)[0].strip()
        if first in LICENSE_NORMALIZE:
            return LICENSE_NORMALIZE[first]
        return first

    if license_raw in LICENSE_NORMALIZE:
        return LICENSE_NORMALIZE[license_raw]
    return license_raw


def get_license_ref(dep_name: str, license_spdx: str, licenses_dir: Path) -> str | None:
    """
    Determine the `see:licenses/xxx` reference for a dependency.
    Returns None for Apache-2.0 (no ref needed) or if no matching license
    file exists.
    """
    if license_spdx == "Apache-2.0":
        return None  # Apache-2.0 deps don't need a see: reference

    # Build candidate names for the license file
    candidates = []

    # Candidate 1: license SPDX name itself (e.g., EPL-1.0, CDDL-1.0)
    candidates.append(license_spdx)

    # Candidate 2: <artifact>-<license> (e.g., logback-classic-EPL-1.0)
    if ":" in dep_name:
        artifact_id = dep_name.split(":")[1]
        safe_artifact = artifact_id.replace(".", "-")
        candidates.append(f"{safe_artifact}-{license_spdx}")
        # Also try groupId last segment
        group_id = dep_name.split(":")[0]
        safe_group = group_id.split(".")[-1]
        candidates.append(f"{safe_group}-{license_spdx}")
    else:
        # npm dep — clean the name
        safe_name = dep_name.replace("@", "").replace("/", "-")
        candidates.append(f"{safe_name}-{license_spdx}")
        # Also try without scope prefix
        if "/" in dep_name:
            short_name = dep_name.split("/")[-1]
            candidates.append(f"{short_name}-{license_spdx}")

    # Check each candidate
    for candidate in candidates:
        if (licenses_dir / candidate).exists():
            return f"see:licenses/{candidate}"

    return None


def sort_key_license(license_spdx: str) -> int:
    """Sort key for license sections."""
    try:
        return LICENSE_ORDER.index(license_spdx)
    except ValueError:
        return len(LICENSE_ORDER)


def format_dep_entry(dep_name: str, version: str, license_spdx: str,
                     license_ref: str | None) -> str:
    """Format a single dependency entry line."""
    base = f"    {dep_name} {version} {license_spdx}"
    if license_ref:
        base += f" {license_ref}"
    return base


def generate_license_file(module: str, deps: list[tuple[str, str, str]],
                        extra_entries: str | None = None,
                        strict: bool = False) -> str:
    """
    Generate the full LICENSE file content.
    If extra_entries is provided, it is appended after the auto-generated
    dependency sections (used for font files, dual-license entries, etc.).
    """
    # Group dependencies by normalized license
    grouped: dict[str, list[tuple[str, str, str]]] = defaultdict(list)
    unknown_deps: list[tuple[str, str]] = []

    for dep_name, license_raw, version in deps:
        # Check overrides first
        if dep_name in LICENSE_OVERRIDES:
            license_spdx = LICENSE_OVERRIDES[dep_name]
        else:
            license_spdx = normalize_license(license_raw)

        if license_spdx == "Unknown":
            unknown_deps.append((dep_name, version))
            continue

        grouped[license_spdx].append((dep_name, license_spdx, version))

    # Sort license sections
    sorted_licenses = sorted(grouped.keys(), key=sort_key_license)

    # Build output
    lines = []
    lines.append(APACHE_LICENSE_HEADER.rstrip())
    lines.append("")
    lines.append(SEATA_SUBCOMPONENTS_HEADER.rstrip())

    for license_spdx in sorted_licenses:
        entries = grouped[license_spdx]
        # Sort entries alphabetically by dep name
        entries.sort(key=lambda x: x[0].lower())

        lines.append("")
        lines.append("=" * 72)
        lines.append(f"{license_spdx} licenses")
        lines.append("=" * 72)
        lines.append("")

        for dep_name, lic, version in entries:
            license_ref = get_license_ref(dep_name, lic, LICENSES_DIR)
            lines.append(format_dep_entry(dep_name, version, lic, license_ref))

    # Report unknown dependencies
    if unknown_deps:
        print(f"\n  WARNING: {len(unknown_deps)} dependencies with unknown licenses:")
        for dep_name, version in sorted(unknown_deps):
            print(f"    - {dep_name} {version}")
        print("  Please add overrides in LICENSE_OVERRIDES dictionary.\n")
        if strict:
            print("ERROR: Unknown licenses are not allowed in strict mode. Exiting.\n")
            sys.exit(1)

    # Append extra entries (font files, dual-license entries, etc.)
    if extra_entries:
        lines.append(extra_entries)

    return "\n".join(lines) + "\n"


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(
        description="Generate LICENSE files for Apache Seata distribution modules"
    )
    parser.add_argument(
        "module",
        choices=["namingserver", "server", "distribution", "all"],
        help="Which module to generate LICENSE for",
    )
    parser.add_argument(
        "--strict",
        action="store_true",
        help="Treat unknown licenses as errors (exit with non-zero code)",
    )
    args = parser.parse_args()

    modules = ["namingserver", "server", "distribution"] if args.module == "all" else [args.module]

    for module in modules:
        config = MODULE_CONFIG[module]
        print(f"\n{'='*60}")
        print(f"Generating LICENSE for: {module}")
        print(f"  Config: {config['config_file'].relative_to(PROJECT_ROOT)}")
        print(f"  Output: {config['output_file'].relative_to(PROJECT_ROOT)}")
        print(f"{'='*60}")

        if not config["config_file"].exists():
            print(f"ERROR: Config file not found: {config['config_file']}")
            sys.exit(1)

        deps = resolve_dependencies(config["config_file"])
        print(f"  Resolved {len(deps)} dependencies")

        extra = DISTRIBUTION_EXTRA_ENTRIES if module == "distribution" else None
        content = generate_license_file(module, deps, extra_entries=extra, strict=args.strict)

        config["output_file"].write_text(content, encoding="utf-8")
        print(f"  Written {config['output_file']} ({len(content)} bytes)")

    print("\nDone!")


if __name__ == "__main__":
    main()
