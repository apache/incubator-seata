/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const fs = require('fs');
const path = require('path');
const childProcess = require('child_process')

// copy seata-saga-statemachine-designer to console
const designerDir = path.join(__dirname, '../../../../../../../saga/seata-saga-statemachine-designer');
if (!fs.existsSync(path.join(designerDir, "dist"))) {
  // if seata-saga-statemachine-designer not build, build this
  childProcess.execSync('cd ' + designerDir + '&& npm install && npm run build')
}

// copy file
const designerDestDir = path.join(__dirname,'../public/saga-statemachine-designer');
if (!fs.existsSync(designerDestDir)) {
  fs.mkdirSync(designerDestDir)
}
fs.readdirSync(path.join(designerDir, 'dist')).forEach(file => {
  fs.copyFileSync(path.join(designerDir, 'dist', file), path.join(designerDestDir, file));
});
fs.renameSync(path.join(designerDestDir, 'index.html'), path.join(designerDestDir, 'designer.html'));
