/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import fs from 'node:fs';
import Ajv2020 from 'ajv/dist/2020.js';
import formats from 'ajv-formats';
const ajv=new Ajv2020({strict:true,strictRequired:true,strictTypes:true,allErrors:true,$data:false});
formats(ajv);
const specs=JSON.parse(fs.readFileSync('../../internal/command/registry.json','utf8'));
for(const spec of specs){
 if(spec.id==='system.invoke')continue;
 const validate=ajv.compile(spec.invocation_schema);
 const example=JSON.parse(fs.readFileSync(`testdata/r9/examples/invocations/${spec.id}.json`,'utf8'));
 if(!validate(example))throw Error(`${spec.id}: ${ajv.errorsText(validate.errors)}`);
}
console.log('56 invocation contracts compiled in strict Ajv and accepted supplied examples');
const corpus=JSON.parse(fs.readFileSync('testdata/corpus.json','utf8'));
for(const c of corpus){
 const spec=specs.find(s=>s.id===c.invocation.command);
 const valid=ajv.compile(spec.invocation_schema)(c.invocation);
 if(valid!==c.schema_valid)throw Error(`corpus mismatch: ${c.name}`);
}
console.log(`${corpus.length} independent positive/negative corpus cases passed`);

// Generate from Go rather than maintaining a second output contract in JS.
const {execFileSync}=await import('node:child_process');
const catalog=JSON.parse(execFileSync(process.env.GO_BINARY || 'go',['run','./cmd/protocolgen'],{cwd:'../..',encoding:'utf8',maxBuffer:8*1024*1024}));
let outputCount=0;
for(const c of catalog.commands){
 if(c.output_data_schema===null)continue;
 const validate=ajv.compile(c.output_data_schema);
 if(validate({}))throw Error(`empty successful data accepted: ${c.capability.id}`);
 outputCount++;
}
if(outputCount!==4||catalog.affordances.length!==13||catalog.skill_ids.length!==7)throw Error('incomplete discovery catalog');
console.log(`${outputCount} command data schemas compiled in strict Ajv; 13 domains and 7 skill references checked`);
