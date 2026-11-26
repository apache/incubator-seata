#!/usr/bin/env python3
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Independent Draft 2020-12 validation of generated contracts and supplied examples."""
import json
from pathlib import Path
from jsonschema import Draft202012Validator, FormatChecker

root = Path(__file__).resolve().parent.parent
envelope = json.loads((root / "schemas/envelope.schema.json").read_text())
Draft202012Validator.check_schema(envelope)

count = 0
fixtures = root / "test/contract/r11/examples/invocations"
schemas = root / "schemas/invocations"
expected_paths = {path.stem + ".schema.json" for path in fixtures.glob("*.json")}
generated_paths = {path.name for path in schemas.glob("*.schema.json")}
assert expected_paths and generated_paths == expected_paths, (generated_paths, expected_paths)
for path in sorted(schemas.glob("*.schema.json")):
    schema = json.loads(path.read_text())
    Draft202012Validator.check_schema(schema)
    command = schema["properties"]["command"]["const"]
    example = json.loads((root / "test/contract/r11/examples/invocations" / (command + ".json")).read_text())
    Draft202012Validator(schema, format_checker=FormatChecker()).validate(example)
    count += 1
print(f"Envelope schema and {count} invocation schemas with independent examples passed Python validation")
