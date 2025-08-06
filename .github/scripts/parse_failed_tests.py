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

import os
import sys
import xml.etree.ElementTree as ET
import json

def find_testcases_with_status(report_dir):
    """
    all xml files under report_dir, parse results
    return dict {(classname, testname): 'passed'/'failed'}
    """
    results = {}

    for root, _, files in os.walk(report_dir):
        for file in files:
            if not file.endswith(".xml"):
                continue
            path = os.path.join(root, file)
            try:
                tree = ET.parse(path)
                root_elem = tree.getroot()
                for testcase in root_elem.iter("testcase"):
                    classname = testcase.attrib.get("classname")
                    testname = testcase.attrib.get("name")

                    failed = testcase.find("failure") is not None or testcase.find("error") is not None
                    status = "failed" if failed else "passed"
                    results[(classname, testname)] = status
            except Exception as e:
                print(f"Warning: Failed to parse {path}: {e}", file=sys.stderr)
    return results


def main():
    if len(sys.argv) != 3:
        print("Usage: parse_failed_tests.py <report_dir_1> <report_dir_2>")
        sys.exit(1)

    report_dir_1 = sys.argv[1]
    report_dir_2 = sys.argv[2]

    results_1 = find_testcases_with_status(report_dir_1)
    results_2 = find_testcases_with_status(report_dir_2)

    flaky_tests = []
    for test_id, status_1 in results_1.items():
        status_2 = results_2.get(test_id, "passed")
        if status_1 == "failed" and status_2 == "passed":
            flaky_tests.append(test_id)

    if flaky_tests:
        print(json.dumps([f"{c}.{t}" for c,t in flaky_tests]))
    else:
        print("[]")

if __name__ == "__main__":
    main()