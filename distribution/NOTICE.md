<!--
    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements.  See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
-->
Due to license compatibility issues, we cannot include jar dependencies such as mysql, mariadb, oracle, etc., in the distribution package.
Please copy database driver dependencies, such as `mysql-connector-java.jar`, to this directory. The following is an example of a directory structure:

```aidl
.
├── DISCLAIMER
├── seata-namingserver
│   ├── Dockerfile
│   ├── LICENSE
│   ├── NOTICE
│   ├── bin
│   │   ├── seata-namingserver-setup.sh
│   │   ├── seata-namingserver.bat
│   │   └── seata-namingserver.sh
│   ├── conf
│   │   ├── application.yml
│   │   ├── logback
│   │   │   ├── console-appender.xml
│   │   │   └── file-appender.xml
│   │   └── logback-spring.xml
│   ├── lib
│   │   ├── caffeine-2.9.3.jar
│   │   ├── checker-qual-3.37.0.jar
│   │   ├── commons-codec-1.15.jar
│   │   ├── commons-compiler-3.1.10.jar
│   │   ├── commons-io-2.8.0.jar
│   │   ├── commons-lang-2.6.jar
│   │   ├── commons-lang3-3.12.0.jar
│   │   ├── error_prone_annotations-2.21.1.jar
│   │   ├── httpasyncclient-4.1.5.jar
│   │   ├── httpclient-4.5.14.jar
│   │   ├── httpcore-4.4.16.jar
│   │   ├── httpcore-nio-4.4.16.jar
│   │   ├── jackson-annotations-2.13.5.jar
│   │   ├── jackson-core-2.13.5.jar
│   │   ├── jackson-databind-2.13.5.jar
│   │   ├── jackson-datatype-jdk8-2.13.5.jar
│   │   ├── jackson-datatype-jsr310-2.13.5.jar
│   │   ├── jackson-module-parameter-names-2.13.5.jar
│   │   ├── jakarta.annotation-api-1.3.5.jar
│   │   ├── janino-3.1.10.jar
│   │   ├── javax.servlet-api-4.0.1.jar
│   │   ├── jjwt-api-0.10.5.jar
│   │   ├── jjwt-impl-0.10.5.jar
│   │   ├── jjwt-jackson-0.10.5.jar
│   │   ├── jul-to-slf4j-1.7.36.jar
│   │   ├── logback-classic-1.2.12.jar
│   │   ├── logback-core-1.2.12.jar
│   │   ├── netty-all-4.1.101.Final.jar
│   │   ├── netty-buffer-4.1.101.Final.jar
│   │   ├── netty-codec-4.1.101.Final.jar
│   │   ├── netty-codec-dns-4.1.101.Final.jar
│   │   ├── netty-codec-haproxy-4.1.101.Final.jar
│   │   ├── netty-codec-http-4.1.101.Final.jar
│   │   ├── netty-codec-http2-4.1.101.Final.jar
│   │   ├── netty-codec-memcache-4.1.101.Final.jar
│   │   ├── netty-codec-mqtt-4.1.101.Final.jar
│   │   ├── netty-codec-redis-4.1.101.Final.jar
│   │   ├── netty-codec-smtp-4.1.101.Final.jar
│   │   ├── netty-codec-socks-4.1.101.Final.jar
│   │   ├── netty-codec-stomp-4.1.101.Final.jar
│   │   ├── netty-codec-xml-4.1.101.Final.jar
│   │   ├── netty-common-4.1.101.Final.jar
│   │   ├── netty-handler-4.1.101.Final.jar
│   │   ├── netty-handler-proxy-4.1.101.Final.jar
│   │   ├── netty-handler-ssl-ocsp-4.1.101.Final.jar
│   │   ├── netty-resolver-4.1.101.Final.jar
│   │   ├── netty-resolver-dns-4.1.101.Final.jar
│   │   ├── netty-resolver-dns-classes-macos-4.1.101.Final.jar
│   │   ├── netty-resolver-dns-native-macos-4.1.101.Final-osx-aarch_64.jar
│   │   ├── netty-resolver-dns-native-macos-4.1.101.Final-osx-x86_64.jar
│   │   ├── netty-transport-4.1.101.Final.jar
│   │   ├── netty-transport-classes-epoll-4.1.101.Final.jar
│   │   ├── netty-transport-classes-kqueue-4.1.101.Final.jar
│   │   ├── netty-transport-native-epoll-4.1.101.Final-linux-aarch_64.jar
│   │   ├── netty-transport-native-epoll-4.1.101.Final-linux-x86_64.jar
│   │   ├── netty-transport-native-kqueue-4.1.101.Final-osx-aarch_64.jar
│   │   ├── netty-transport-native-kqueue-4.1.101.Final-osx-x86_64.jar
│   │   ├── netty-transport-native-unix-common-4.1.101.Final.jar
│   │   ├── netty-transport-rxtx-4.1.101.Final.jar
│   │   ├── netty-transport-sctp-4.1.101.Final.jar
│   │   ├── netty-transport-udt-4.1.101.Final.jar
│   │   ├── seata-common-2.5.0.jar
│   │   ├── seata-console-2.5.0.jar
│   │   ├── slf4j-api-1.7.36.jar
│   │   ├── snakeyaml-2.0.jar
│   │   ├── spring-aop-5.3.39.jar
│   │   ├── spring-beans-5.3.39.jar
│   │   ├── spring-boot-2.7.18.jar
│   │   ├── spring-boot-autoconfigure-2.7.18.jar
│   │   ├── spring-boot-starter-2.7.18.jar
│   │   ├── spring-boot-starter-json-2.7.18.jar
│   │   ├── spring-boot-starter-logging-2.7.18.jar
│   │   ├── spring-boot-starter-security-2.7.18.jar
│   │   ├── spring-boot-starter-tomcat-2.7.18.jar
│   │   ├── spring-boot-starter-web-2.7.18.jar
│   │   ├── spring-context-5.3.39.jar
│   │   ├── spring-core-5.3.39.jar
│   │   ├── spring-expression-5.3.39.jar
│   │   ├── spring-jcl-5.3.39.jar
│   │   ├── spring-security-config-5.7.11.jar
│   │   ├── spring-security-core-5.7.11.jar
│   │   ├── spring-security-crypto-5.7.11.jar
│   │   ├── spring-security-web-5.7.11.jar
│   │   ├── spring-web-5.3.39.jar
│   │   ├── spring-webmvc-5.3.39.jar
│   │   ├── tomcat-annotations-api-9.0.83.jar
│   │   ├── tomcat-embed-core-9.0.106.jar
│   │   ├── tomcat-embed-el-9.0.106.jar
│   │   └── tomcat-embed-websocket-9.0.106.jar
│   ├── licenses
│   │   ├── Apache-1.1
│   │   ├── CDDL+GPL-1.1
│   │   ├── CDDL-1.0
│   │   ├── EPL-1.0
│   │   ├── EPL-2.0
│   │   ├── Python-2.0
│   │   ├── abego-treelayout-BSD-3-Clause
│   │   ├── alicloud-console-components-MIT
│   │   ├── alicloud-console-components-actions-MIT
│   │   ├── alifd-field-MIT
│   │   ├── alifd-next-MIT
│   │   ├── alifd-validate-MIT
│   │   ├── ansi-colors-MIT
│   │   ├── ansi-regex-MIT
│   │   ├── ansi-styles-MIT
│   │   ├── antlr-stringtemplate3-BSD-3-Clause
│   │   ├── antlr2-BSD-3-Clause
│   │   ├── antlr3-BSD
│   │   ├── antlr4-BSD
│   │   ├── antlr4-ST4-BSD
│   │   ├── anymatch-ISC
│   │   ├── argparse-MIT
│   │   ├── asm-BSD-3-Clause
│   │   ├── asn1.js-MIT
│   │   ├── asynckit-MIT
│   │   ├── axios-MIT
│   │   ├── babel-code-frame-MIT
│   │   ├── babel-compat-data-MIT
│   │   ├── babel-core-MIT
│   │   ├── babel-generator-MIT
│   │   ├── babel-helper-annotate-as-pure-MIT
│   │   ├── babel-helper-compilation-targets-MIT
│   │   ├── babel-helper-environment-visitor-MIT
│   │   ├── babel-helper-function-name-MIT
│   │   ├── babel-helper-hoist-variables-MIT
│   │   ├── babel-helper-module-imports-MIT
│   │   ├── babel-helper-module-transforms-MIT
│   │   ├── babel-helper-plugin-utils-MIT
│   │   ├── babel-helper-simple-access-MIT
│   │   ├── babel-helper-split-export-declaration-MIT
│   │   ├── babel-helper-string-parser-MIT
│   │   ├── babel-helper-validator-identifier-MIT
│   │   ├── babel-helper-validator-option-MIT
│   │   ├── babel-helpers-MIT
│   │   ├── babel-highlight-MIT
│   │   ├── babel-parser-MIT
│   │   ├── babel-plugin-emotion-MIT
│   │   ├── babel-plugin-macros-MIT
│   │   ├── babel-plugin-styled-components-MIT
│   │   ├── babel-plugin-syntax-jsx-MIT
│   │   ├── babel-runtime-MIT
│   │   ├── babel-template-MIT
│   │   ├── babel-traverse-MIT
│   │   ├── babel-types-MIT
│   │   ├── balanced-match-MIT
│   │   ├── bignumber.js-MIT
│   │   ├── bn.js-MIT
│   │   ├── bpmn-font-SIL
│   │   ├── bpmn-io-cm-theme-MIT
│   │   ├── bpmn-io-diagram-js-ui-MIT
│   │   ├── bpmn-io-feel-editor-MIT
│   │   ├── bpmn-io-feel-lint-MIT
│   │   ├── bpmn-io-properties-panel-MIT
│   │   ├── brace-expansion-MIT
│   │   ├── braces-2.3.1-MIT
│   │   ├── braces-3.0.2-MIT
│   │   ├── braces-MIT
│   │   ├── brorand-MIT
│   │   ├── browser-stdout-ISC
│   │   ├── browserify-aes-MIT
│   │   ├── browserify-rsa-MIT
│   │   ├── browserify-sign-ISC
│   │   ├── browserslist-MIT
│   │   ├── buffer-xor-MIT
│   │   ├── callsites-MIT
│   │   ├── camelcase-MIT
│   │   ├── camelize-MIT
│   │   ├── chalk-MIT
│   │   ├── checker-qual-MIT
│   │   ├── chokidar-MIT
│   │   ├── cipher-base-MIT
│   │   ├── classnames-2.5.1-MIT
│   │   ├── classnames-MIT
│   │   ├── cliui-ISC
│   │   ├── clsx-MIT
│   │   ├── codemirror-autocomplete-MIT
│   │   ├── codemirror-commands-MIT
│   │   ├── codemirror-language-MIT
│   │   ├── codemirror-lint-MIT
│   │   ├── codemirror-state-MIT
│   │   ├── codemirror-view-MIT
│   │   ├── color-convert-MIT
│   │   ├── color-name-MIT
│   │   ├── combined-stream-MIT
│   │   ├── component-event-MIT
│   │   ├── concat-map-MIT
│   │   ├── convert-source-map-MIT
│   │   ├── core-js-MIT
│   │   ├── core-util-is-MIT
│   │   ├── cosmiconfig-MIT
│   │   ├── create-hash-MIT
│   │   ├── create-hmac-MIT
│   │   ├── crelt-MIT
│   │   ├── css-color-keywords-ISC
│   │   ├── css-to-react-native-MIT
│   │   ├── csstype-MIT
│   │   ├── dayjs-MIT
│   │   ├── debug-MIT
│   │   ├── decamelize-MIT
│   │   ├── decode-uri-component-MIT
│   │   ├── delayed-stream-MIT
│   │   ├── dexx-collections-MIT
│   │   ├── diagram-js-MIT
│   │   ├── diagram-js-grid-MIT
│   │   ├── didi-MIT
│   │   ├── dom-helpers-MIT
│   │   ├── dom-walk-MIT
│   │   ├── dom7-MIT
│   │   ├── domify-MIT
│   │   ├── driver-dom-BSD-3-Clause
│   │   ├── driver-miniapp-BSD-3-Clause
│   │   ├── driver-universal-BSD-3-Clause
│   │   ├── driver-weex-BSD-3-Clause
│   │   ├── dva-MIT
│   │   ├── dva-core-MIT
│   │   ├── electron-to-chromium-ISC
│   │   ├── elliptic-MIT
│   │   ├── emotion-cache-MIT
│   │   ├── emotion-core-MIT
│   │   ├── emotion-css-MIT
│   │   ├── emotion-hash-MIT
│   │   ├── emotion-is-prop-valid-MIT
│   │   ├── emotion-memoize-MIT
│   │   ├── emotion-serialize-MIT
│   │   ├── emotion-sheet-MIT
│   │   ├── emotion-stylis-MIT
│   │   ├── emotion-unitless-MIT
│   │   ├── emotion-utils-MIT
│   │   ├── emotion-weak-memoize-MIT
│   │   ├── encoding-MIT
│   │   ├── error-ex-MIT
│   │   ├── escalade-MIT
│   │   ├── escape-string-regexp-MIT
│   │   ├── evp_bytestokey-MIT
│   │   ├── feelers-MIT
│   │   ├── feelin-MIT
│   │   ├── fill-range-MIT
│   │   ├── find-root-MIT
│   │   ├── find-up-MIT
│   │   ├── flat-BSD-3-Clause
│   │   ├── flatten-MIT
│   │   ├── focus-trap-MIT
│   │   ├── follow-redirects-MIT
│   │   ├── form-data-MIT
│   │   ├── fs.realpath-ISC
│   │   ├── fsevents-MIT
│   │   ├── function-bind-MIT
│   │   ├── gensync-MIT
│   │   ├── get-caller-file-ISC
│   │   ├── glob-ISC
│   │   ├── glob-parent-ISC
│   │   ├── global-MIT
│   │   ├── globals-MIT
│   │   ├── growl-MIT
│   │   ├── h2-MPL-2.0
│   │   ├── hamcrest-BSD-3-Clause
│   │   ├── hammerjs-MIT
│   │   ├── has-flag-MIT
│   │   ├── hash-base-MIT
│   │   ├── hash.js-MIT
│   │   ├── hasown-MIT
│   │   ├── he-MIT
│   │   ├── history-MIT
│   │   ├── hmac-drbg-MIT
│   │   ├── hoist-non-react-statics-BSD-3-Clause
│   │   ├── iconv-lite-MIT
│   │   ├── icu4j-Unicode
│   │   ├── import-fresh-MIT
│   │   ├── inflight-ISC
│   │   ├── inherits-ISC
│   │   ├── inherits-browser-ISC
│   │   ├── invariant-MIT
│   │   ├── is-arrayish-MIT
│   │   ├── is-binary-path-MIT
│   │   ├── is-core-module-MIT
│   │   ├── is-extglob-MIT
│   │   ├── is-fullwidth-code-point-MIT
│   │   ├── is-glob-MIT
│   │   ├── is-number-MIT
│   │   ├── is-plain-obj-MIT
│   │   ├── is-plain-object-MIT
│   │   ├── is-what-MIT
│   │   ├── isarray-MIT
│   │   ├── isexe-ISC
│   │   ├── isobject-MIT
│   │   ├── isomorphic-fetch-MIT
│   │   ├── janino-BSD-3-Clause
│   │   ├── jedis-MIT
│   │   ├── jquery-MIT
│   │   ├── jridgewell-gen-mapping-MIT
│   │   ├── jridgewell-resolve-uri-MIT
│   │   ├── jridgewell-set-array-MIT
│   │   ├── jridgewell-sourcemap-codec-MIT
│   │   ├── jridgewell-trace-mapping-MIT
│   │   ├── js-tokens-MIT
│   │   ├── jsesc-MIT
│   │   ├── json-parse-even-better-errors-MIT
│   │   ├── json5-MIT
│   │   ├── jul-to-slf4j-MIT
│   │   ├── junit4-EPL-1.0
│   │   ├── kryo-BSD-3-Clause
│   │   ├── lang-feel-MIT
│   │   ├── lezer-common-MIT
│   │   ├── lezer-feel-MIT
│   │   ├── lezer-highlight-MIT
│   │   ├── lezer-lr-MIT
│   │   ├── lezer-markdown-MIT
│   │   ├── lines-and-columns-MIT
│   │   ├── loader-utils-MIT
│   │   ├── locate-path-MIT
│   │   ├── lodash-MIT
│   │   ├── lodash-es-MIT
│   │   ├── lodash.clonedeep-MIT
│   │   ├── log-symbols-MIT
│   │   ├── loose-envify-MIT
│   │   ├── lru-cache-ISC
│   │   ├── luxon-MIT
│   │   ├── md5.js-MIT
│   │   ├── memoize-one-MIT
│   │   ├── merge-anything-MIT
│   │   ├── mime-db-MIT
│   │   ├── mime-types-MIT
│   │   ├── min-dash-MIT
│   │   ├── min-document-MIT
│   │   ├── min-dom-MIT
│   │   ├── minimalistic-assert-ISC
│   │   ├── minimalistic-crypto-utils-MIT
│   │   ├── minimatch-ISC
│   │   ├── minlog-BSD-3-Clause
│   │   ├── mocha-MIT
│   │   ├── moment-MIT
│   │   ├── ms-MIT
│   │   ├── mxparser-IUELSL
│   │   ├── nanoid-MIT
│   │   ├── node-fetch-MIT
│   │   ├── node-releases-MIT
│   │   ├── normalize-path-MIT
│   │   ├── object-assign-MIT
│   │   ├── object-refs-MIT
│   │   ├── omit.js-MIT
│   │   ├── once-ISC
│   │   ├── p-limit-MIT
│   │   ├── p-locate-MIT
│   │   ├── parent-module-MIT
│   │   ├── parse-asn1-ISC
│   │   ├── parse-json-MIT
│   │   ├── path-exists-MIT
│   │   ├── path-intersection-MIT
│   │   ├── path-is-absolute-MIT
│   │   ├── path-parse-MIT
│   │   ├── path-to-regexp-MIT
│   │   ├── path-type-MIT
│   │   ├── pbkdf2-MIT
│   │   ├── picocolors-ISC
│   │   ├── picomatch-MIT
│   │   ├── postcss-value-parse-MIT
│   │   ├── postcss-value-parser-MIT
│   │   ├── postgresql-BSD-2-Clause
│   │   ├── preact-MIT
│   │   ├── process-MIT
│   │   ├── process-nextick-args-MIT
│   │   ├── prop-types-MIT
│   │   ├── protobuf-java-BSD-3-Clause
│   │   ├── proxy-from-env-MIT
│   │   ├── randombytes-MIT
│   │   ├── rax-BSD-3-Clause
│   │   ├── react-MIT
│   │   ├── react-dom-MIT
│   │   ├── react-is-MIT
│   │   ├── react-lifecycles-compat-MIT
│   │   ├── react-loading-skeleton-MIT
│   │   ├── react-redux-MIT
│   │   ├── react-router-MIT
│   │   ├── react-router-dom-MIT
│   │   ├── react-router-redux-MIT
│   │   ├── react-transition-group-BSD-3-Clause
│   │   ├── readable-stream-MIT
│   │   ├── readdirp-MIT
│   │   ├── redux-MIT
│   │   ├── redux-saga-MIT
│   │   ├── redux-thunk-MIT
│   │   ├── reflectasm-BSD-3-Clause
│   │   ├── regenerator-runtime-MIT
│   │   ├── require-directory-MIT
│   │   ├── resize-observer-polyfill-MIT
│   │   ├── resolve-MIT
│   │   ├── resolve-from-MIT
│   │   ├── resolve-pathname-MIT
│   │   ├── ripemd160-MIT
│   │   ├── safe-buffer-MIT
│   │   ├── safer-buffer-MIT
│   │   ├── scheduler-MIT
│   │   ├── semver-ISC
│   │   ├── serialize-javascript-BSD-3-Clause
│   │   ├── sha.js-MIT
│   │   ├── shallow-element-equals-MIT
│   │   ├── slf4j-api-MIT
│   │   ├── source-map-BSD-3-Clause
│   │   ├── sprintf-js-BSD-3-Clause
│   │   ├── ssr-window-MIT
│   │   ├── string-decoder-MIT
│   │   ├── string-width-MIT
│   │   ├── string_decoder-MIT
│   │   ├── strip-ansi-MIT
│   │   ├── strip-json-comments-MIT
│   │   ├── style-equal-MIT
│   │   ├── style-mod-MIT
│   │   ├── styled-components-MIT
│   │   ├── stylis-MIT
│   │   ├── stylis-rule-sheet-MIT
│   │   ├── supports-color-MIT
│   │   ├── supports-preserve-symlinks-flag-MIT
│   │   ├── svelte-MIT
│   │   ├── swiper-MIT
│   │   ├── symbol-observable-MIT
│   │   ├── tabbable-MIT
│   │   ├── tiny-invariant-MIT
│   │   ├── tiny-svg-MIT
│   │   ├── tiny-warning-MIT
│   │   ├── to-fast-properties-MIT
│   │   ├── to-regex-range-MIT
│   │   ├── tr46-MIT
│   │   ├── tslib-OBSD
│   │   ├── types-history-MIT
│   │   ├── types-hoist-non-react-statics-MIT
│   │   ├── types-isomorphic-fetch-MIT
│   │   ├── types-parse-json-MIT
│   │   ├── types-prop-types-MIT
│   │   ├── types-react-MIT
│   │   ├── types-react-dom-MIT
│   │   ├── types-react-router-MIT
│   │   ├── types-react-router-dom-MIT
│   │   ├── types-react-router-redux-MIT
│   │   ├── types-scheduler-MIT
│   │   ├── types-use-sync-external-store-MIT
│   │   ├── ungap-ISC
│   │   ├── uni-BSD-3-Clause
│   │   ├── universal-BSD-3-Clause
│   │   ├── update-browserslist-db-MIT
│   │   ├── use-sync-external-store-MIT
│   │   ├── util-deprecate-MIT
│   │   ├── value-equal-MIT
│   │   ├── w3c-keyname-MIT
│   │   ├── warning-BSD-3-Clause
│   │   ├── warning-MIT
│   │   ├── webidl-conversions-BSD-2-Clause
│   │   ├── whatwg-fetch-MIT
│   │   ├── whatwg-url-MIT
│   │   ├── which-ISC
│   │   ├── wide-align-ISC
│   │   ├── wrap-ansi-MIT
│   │   ├── wrappy-ISC
│   │   ├── xstream-BSD-3-Clause
│   │   ├── y18n-ISC
│   │   ├── yallist-ISC
│   │   ├── yaml-ISC
│   │   ├── yamljs-MIT
│   │   ├── yargs-MIT
│   │   ├── yargs-parser-ISC
│   │   ├── yargs-unparser-MIT
│   │   ├── yocto-queue-MIT
│   │   └── zstd-jni-BSD-2-Clause
│   └── target
│       └── seata-namingserver.jar
└── seata-server
    ├── Dockerfile
    ├── LICENSE
    ├── NOTICE
    ├── bin
    │   ├── seata-server.bat
    │   ├── seata-server.sh
    │   └── seata-setup.sh
    ├── conf
    │   ├── application.example.yml
    │   ├── application.raft.example.yml
    │   ├── application.yml
    │   ├── logback
    │   │   ├── console-appender.xml
    │   │   ├── file-appender.xml
    │   │   ├── kafka-appender.xml
    │   │   ├── logstash-appender.xml
    │   │   └── metric-appender.xml
    │   └── logback-spring.xml
    ├── ext
    │   └── apm-skywalking
    │       ├── plugins
    │       │   ├── apm-jdbc-commons-8.6.0.jar
    │       │   ├── apm-mysql-5.x-plugin-8.6.0.jar
    │       │   ├── apm-mysql-6.x-plugin-8.6.0.jar
    │       │   ├── apm-mysql-8.x-plugin-8.6.0.jar
    │       │   ├── apm-mysql-commons-8.6.0.jar
    │       │   └── apm-seata-skywalking-plugin-2.5.0.jar
    │       └── skywalking-agent.jar
    ├── lib
    │   ├── DmJdbcDriver18-8.1.2.192.jar
    │   ├── HikariCP-4.0.3.jar
    │   ├── ant-1.10.12.jar
    │   ├── ant-launcher-1.10.12.jar
    │   ├── aopalliance-1.0.jar
    │   ├── apollo-client-2.0.1.jar
    │   ├── apollo-core-2.0.1.jar
    │   ├── archaius-core-0.7.6.jar
    │   ├── asm-6.0.jar
    │   ├── audience-annotations-0.12.0.jar
    │   ├── bolt-1.6.7.jar
    │   ├── bucket4j_jdk8-core-8.1.0.jar
    │   ├── checker-qual-3.37.0.jar
    │   ├── commons-codec-1.15.jar
    │   ├── commons-compiler-3.1.10.jar
    │   ├── commons-configuration-1.10.jar
    │   ├── commons-dbcp2-2.9.0.jar
    │   ├── commons-io-2.8.0.jar
    │   ├── commons-jxpath-1.3.jar
    │   ├── commons-lang-2.6.jar
    │   ├── commons-logging-1.2.jar
    │   ├── commons-math-2.2.jar
    │   ├── commons-pool-1.6.jar
    │   ├── commons-pool2-2.11.1.jar
    │   ├── compactmap-2.0.jar
    │   ├── config-1.2.1.jar
    │   ├── consul-api-1.4.2.jar
    │   ├── curator-client-5.1.0.jar
    │   ├── curator-framework-5.1.0.jar
    │   ├── curator-recipes-5.1.0.jar
    │   ├── curator-test-5.1.0.jar
    │   ├── dexx-collections-0.2.jar
    │   ├── disruptor-3.3.7.jar
    │   ├── druid-1.2.20.jar
    │   ├── error_prone_annotations-2.21.1.jar
    │   ├── eureka-client-1.10.18.jar
    │   ├── failsafe-2.3.3.jar
    │   ├── failureaccess-1.0.1.jar
    │   ├── fastjson-1.2.83.jar
    │   ├── fastjson2-2.0.52.jar
    │   ├── fury-core-0.8.0.jar
    │   ├── grpc-api-1.55.1.jar
    │   ├── grpc-context-1.55.1.jar
    │   ├── grpc-grpclb-1.27.1.jar
    │   ├── grpc-netty-1.55.1.jar
    │   ├── grpc-protobuf-1.55.1.jar
    │   ├── grpc-protobuf-lite-1.55.1.jar
    │   ├── grpc-stub-1.55.1.jar
    │   ├── gson-2.9.1.jar
    │   ├── guava-32.1.3-jre.jar
    │   ├── guice-5.0.1.jar
    │   ├── hamcrest-2.2.jar
    │   ├── hamcrest-core-2.2.jar
    │   ├── hessian-4.0.3.jar
    │   ├── hessian-4.0.63.jar
    │   ├── httpasyncclient-4.1.5.jar
    │   ├── httpclient-4.5.14.jar
    │   ├── httpcore-4.4.16.jar
    │   ├── httpcore-nio-4.4.16.jar
    │   ├── j2objc-annotations-2.8.jar
    │   ├── jackson-annotations-2.13.5.jar
    │   ├── jackson-core-2.13.5.jar
    │   ├── jackson-databind-2.13.5.jar
    │   ├── jakarta.annotation-api-1.3.5.jar
    │   ├── janino-3.1.10.jar
    │   ├── javax.inject-1.jar
    │   ├── javax.servlet-api-4.0.1.jar
    │   ├── jcommander-1.82.jar
    │   ├── jctools-core-2.1.1.jar
    │   ├── jdbc
    │   │   └── NOTICE.md
    │   ├── jedis-3.8.0.jar
    │   ├── jersey-apache-client4-1.19.1.jar
    │   ├── jersey-client-1.19.1.jar
    │   ├── jersey-core-1.19.1.jar
    │   ├── jetcd-common-0.5.0.jar
    │   ├── jetcd-core-0.5.0.jar
    │   ├── jetcd-resolver-0.5.0.jar
    │   ├── jettison-1.5.4.jar
    │   ├── jna-5.5.0.jar
    │   ├── joda-time-2.3.jar
    │   ├── jraft-core-1.3.14.jar
    │   ├── jsr305-3.0.2.jar
    │   ├── jsr311-api-1.1.1.jar
    │   ├── jul-to-slf4j-1.7.36.jar
    │   ├── junit-4.13.2.jar
    │   ├── kafka-clients-3.6.1.jar
    │   ├── kryo-5.4.0.jar
    │   ├── kryo-serializers-0.45.jar
    │   ├── logback-classic-1.2.12.jar
    │   ├── logback-core-1.2.12.jar
    │   ├── logback-kafka-appender-0.2.0-RC2.jar
    │   ├── logstash-logback-encoder-6.5.jar
    │   ├── lz4-java-1.7.1.jar
    │   ├── metrics-core-4.2.22.jar
    │   ├── minlog-1.3.1.jar
    │   ├── mxparser-1.2.2.jar
    │   ├── nacos-api-1.4.6.jar
    │   ├── nacos-client-1.4.6.jar
    │   ├── nacos-common-1.4.6.jar
    │   ├── netflix-eventbus-0.3.0.jar
    │   ├── netflix-infix-0.3.0.jar
    │   ├── netty-all-4.1.101.Final.jar
    │   ├── netty-buffer-4.1.101.Final.jar
    │   ├── netty-codec-4.1.101.Final.jar
    │   ├── netty-codec-dns-4.1.101.Final.jar
    │   ├── netty-codec-haproxy-4.1.101.Final.jar
    │   ├── netty-codec-http-4.1.101.Final.jar
    │   ├── netty-codec-http2-4.1.101.Final.jar
    │   ├── netty-codec-memcache-4.1.101.Final.jar
    │   ├── netty-codec-mqtt-4.1.101.Final.jar
    │   ├── netty-codec-redis-4.1.101.Final.jar
    │   ├── netty-codec-smtp-4.1.101.Final.jar
    │   ├── netty-codec-socks-4.1.101.Final.jar
    │   ├── netty-codec-stomp-4.1.101.Final.jar
    │   ├── netty-codec-xml-4.1.101.Final.jar
    │   ├── netty-common-4.1.101.Final.jar
    │   ├── netty-handler-4.1.101.Final.jar
    │   ├── netty-handler-proxy-4.1.101.Final.jar
    │   ├── netty-handler-ssl-ocsp-4.1.101.Final.jar
    │   ├── netty-resolver-4.1.101.Final.jar
    │   ├── netty-resolver-dns-4.1.101.Final.jar
    │   ├── netty-resolver-dns-classes-macos-4.1.101.Final.jar
    │   ├── netty-resolver-dns-native-macos-4.1.101.Final-osx-aarch_64.jar
    │   ├── netty-resolver-dns-native-macos-4.1.101.Final-osx-x86_64.jar
    │   ├── netty-transport-4.1.101.Final.jar
    │   ├── netty-transport-classes-epoll-4.1.101.Final.jar
    │   ├── netty-transport-classes-kqueue-4.1.101.Final.jar
    │   ├── netty-transport-native-epoll-4.1.101.Final-linux-aarch_64.jar
    │   ├── netty-transport-native-epoll-4.1.101.Final-linux-x86_64.jar
    │   ├── netty-transport-native-kqueue-4.1.101.Final-osx-aarch_64.jar
    │   ├── netty-transport-native-kqueue-4.1.101.Final-osx-x86_64.jar
    │   ├── netty-transport-native-unix-common-4.1.101.Final.jar
    │   ├── netty-transport-rxtx-4.1.101.Final.jar
    │   ├── netty-transport-sctp-4.1.101.Final.jar
    │   ├── netty-transport-udt-4.1.101.Final.jar
    │   ├── objenesis-3.2.jar
    │   ├── perfmark-api-0.25.0.jar
    │   ├── postgresql-42.3.8.jar
    │   ├── proto-google-common-protos-2.9.0.jar
    │   ├── protobuf-java-3.25.5.jar
    │   ├── protobuf-java-util-3.11.0.jar
    │   ├── reflectasm-1.11.9.jar
    │   ├── registry-client-all-6.3.0.jar
    │   ├── rocksdbjni-8.8.1.jar
    │   ├── seata-common-2.5.0.jar
    │   ├── seata-compressor-all-2.5.0.jar
    │   ├── seata-compressor-bzip2-2.5.0.jar
    │   ├── seata-compressor-deflater-2.5.0.jar
    │   ├── seata-compressor-gzip-2.5.0.jar
    │   ├── seata-compressor-lz4-2.5.0.jar
    │   ├── seata-compressor-zip-2.5.0.jar
    │   ├── seata-compressor-zstd-2.5.0.jar
    │   ├── seata-config-all-2.5.0.jar
    │   ├── seata-config-apollo-2.5.0.jar
    │   ├── seata-config-consul-2.5.0.jar
    │   ├── seata-config-core-2.5.0.jar
    │   ├── seata-config-etcd3-2.5.0.jar
    │   ├── seata-config-nacos-2.5.0.jar
    │   ├── seata-config-spring-cloud-2.5.0.jar
    │   ├── seata-config-zk-2.5.0.jar
    │   ├── seata-core-2.5.0.jar
    │   ├── seata-discovery-all-2.5.0.jar
    │   ├── seata-discovery-consul-2.5.0.jar
    │   ├── seata-discovery-core-2.5.0.jar
    │   ├── seata-discovery-custom-2.5.0.jar
    │   ├── seata-discovery-etcd3-2.5.0.jar
    │   ├── seata-discovery-eureka-2.5.0.jar
    │   ├── seata-discovery-nacos-2.5.0.jar
    │   ├── seata-discovery-namingserver-2.5.0.jar
    │   ├── seata-discovery-redis-2.5.0.jar
    │   ├── seata-discovery-sofa-2.5.0.jar
    │   ├── seata-discovery-zk-2.5.0.jar
    │   ├── seata-metrics-all-2.5.0.jar
    │   ├── seata-metrics-api-2.5.0.jar
    │   ├── seata-metrics-core-2.5.0.jar
    │   ├── seata-metrics-exporter-prometheus-2.5.0.jar
    │   ├── seata-metrics-registry-compact-2.5.0.jar
    │   ├── seata-serializer-all-2.5.0.jar
    │   ├── seata-serializer-fastjson2-2.5.0.jar
    │   ├── seata-serializer-fury-2.5.0.jar
    │   ├── seata-serializer-hessian-2.5.0.jar
    │   ├── seata-serializer-kryo-2.5.0.jar
    │   ├── seata-serializer-protobuf-2.5.0.jar
    │   ├── seata-serializer-seata-2.5.0.jar
    │   ├── seata-spring-autoconfigure-core-2.5.0.jar
    │   ├── seata-spring-autoconfigure-server-2.5.0.jar
    │   ├── servo-core-0.12.21.jar
    │   ├── simpleclient-0.15.0.jar
    │   ├── simpleclient_common-0.15.0.jar
    │   ├── simpleclient_httpserver-0.15.0.jar
    │   ├── simpleclient_tracer_common-0.15.0.jar
    │   ├── simpleclient_tracer_otel-0.15.0.jar
    │   ├── simpleclient_tracer_otel_agent-0.15.0.jar
    │   ├── slf4j-api-1.7.36.jar
    │   ├── snakeyaml-2.0.jar
    │   ├── snappy-java-1.1.10.5.jar
    │   ├── sofa-common-tools-1.0.12.jar
    │   ├── spring-aop-5.3.39.jar
    │   ├── spring-beans-5.3.39.jar
    │   ├── spring-boot-2.7.18.jar
    │   ├── spring-boot-autoconfigure-2.7.18.jar
    │   ├── spring-boot-starter-2.7.18.jar
    │   ├── spring-boot-starter-logging-2.7.18.jar
    │   ├── spring-context-5.3.39.jar
    │   ├── spring-core-5.3.39.jar
    │   ├── spring-expression-5.3.39.jar
    │   ├── spring-jcl-5.3.39.jar
    │   ├── spring-test-5.3.39.jar
    │   ├── spring-web-5.3.39.jar
    │   ├── xstream-1.4.21.jar
    │   ├── zookeeper-3.7.2.jar
    │   ├── zookeeper-jute-3.7.2.jar
    │   └── zstd-jni-1.5.0-4.jar
    ├── licenses
    │   ├── Apache-1.1
    │   ├── CDDL+GPL-1.1
    │   ├── CDDL-1.0
    │   ├── EPL-1.0
    │   ├── EPL-2.0
    │   ├── Python-2.0
    │   ├── abego-treelayout-BSD-3-Clause
    │   ├── alicloud-console-components-MIT
    │   ├── alicloud-console-components-actions-MIT
    │   ├── alifd-field-MIT
    │   ├── alifd-next-MIT
    │   ├── alifd-validate-MIT
    │   ├── ansi-colors-MIT
    │   ├── ansi-regex-MIT
    │   ├── ansi-styles-MIT
    │   ├── antlr-stringtemplate3-BSD-3-Clause
    │   ├── antlr2-BSD-3-Clause
    │   ├── antlr3-BSD
    │   ├── antlr4-BSD
    │   ├── antlr4-ST4-BSD
    │   ├── anymatch-ISC
    │   ├── argparse-MIT
    │   ├── asm-BSD-3-Clause
    │   ├── asn1.js-MIT
    │   ├── asynckit-MIT
    │   ├── axios-MIT
    │   ├── babel-code-frame-MIT
    │   ├── babel-compat-data-MIT
    │   ├── babel-core-MIT
    │   ├── babel-generator-MIT
    │   ├── babel-helper-annotate-as-pure-MIT
    │   ├── babel-helper-compilation-targets-MIT
    │   ├── babel-helper-environment-visitor-MIT
    │   ├── babel-helper-function-name-MIT
    │   ├── babel-helper-hoist-variables-MIT
    │   ├── babel-helper-module-imports-MIT
    │   ├── babel-helper-module-transforms-MIT
    │   ├── babel-helper-plugin-utils-MIT
    │   ├── babel-helper-simple-access-MIT
    │   ├── babel-helper-split-export-declaration-MIT
    │   ├── babel-helper-string-parser-MIT
    │   ├── babel-helper-validator-identifier-MIT
    │   ├── babel-helper-validator-option-MIT
    │   ├── babel-helpers-MIT
    │   ├── babel-highlight-MIT
    │   ├── babel-parser-MIT
    │   ├── babel-plugin-emotion-MIT
    │   ├── babel-plugin-macros-MIT
    │   ├── babel-plugin-styled-components-MIT
    │   ├── babel-plugin-syntax-jsx-MIT
    │   ├── babel-runtime-MIT
    │   ├── babel-template-MIT
    │   ├── babel-traverse-MIT
    │   ├── babel-types-MIT
    │   ├── balanced-match-MIT
    │   ├── bignumber.js-MIT
    │   ├── bn.js-MIT
    │   ├── bpmn-font-SIL
    │   ├── bpmn-io-cm-theme-MIT
    │   ├── bpmn-io-diagram-js-ui-MIT
    │   ├── bpmn-io-feel-editor-MIT
    │   ├── bpmn-io-feel-lint-MIT
    │   ├── bpmn-io-properties-panel-MIT
    │   ├── brace-expansion-MIT
    │   ├── braces-2.3.1-MIT
    │   ├── braces-3.0.2-MIT
    │   ├── braces-MIT
    │   ├── brorand-MIT
    │   ├── browser-stdout-ISC
    │   ├── browserify-aes-MIT
    │   ├── browserify-rsa-MIT
    │   ├── browserify-sign-ISC
    │   ├── browserslist-MIT
    │   ├── buffer-xor-MIT
    │   ├── callsites-MIT
    │   ├── camelcase-MIT
    │   ├── camelize-MIT
    │   ├── chalk-MIT
    │   ├── checker-qual-MIT
    │   ├── chokidar-MIT
    │   ├── cipher-base-MIT
    │   ├── classnames-2.5.1-MIT
    │   ├── classnames-MIT
    │   ├── cliui-ISC
    │   ├── clsx-MIT
    │   ├── codemirror-autocomplete-MIT
    │   ├── codemirror-commands-MIT
    │   ├── codemirror-language-MIT
    │   ├── codemirror-lint-MIT
    │   ├── codemirror-state-MIT
    │   ├── codemirror-view-MIT
    │   ├── color-convert-MIT
    │   ├── color-name-MIT
    │   ├── combined-stream-MIT
    │   ├── component-event-MIT
    │   ├── concat-map-MIT
    │   ├── convert-source-map-MIT
    │   ├── core-js-MIT
    │   ├── core-util-is-MIT
    │   ├── cosmiconfig-MIT
    │   ├── create-hash-MIT
    │   ├── create-hmac-MIT
    │   ├── crelt-MIT
    │   ├── css-color-keywords-ISC
    │   ├── css-to-react-native-MIT
    │   ├── csstype-MIT
    │   ├── dayjs-MIT
    │   ├── debug-MIT
    │   ├── decamelize-MIT
    │   ├── decode-uri-component-MIT
    │   ├── delayed-stream-MIT
    │   ├── dexx-collections-MIT
    │   ├── diagram-js-MIT
    │   ├── diagram-js-grid-MIT
    │   ├── didi-MIT
    │   ├── dom-helpers-MIT
    │   ├── dom-walk-MIT
    │   ├── dom7-MIT
    │   ├── domify-MIT
    │   ├── driver-dom-BSD-3-Clause
    │   ├── driver-miniapp-BSD-3-Clause
    │   ├── driver-universal-BSD-3-Clause
    │   ├── driver-weex-BSD-3-Clause
    │   ├── dva-MIT
    │   ├── dva-core-MIT
    │   ├── electron-to-chromium-ISC
    │   ├── elliptic-MIT
    │   ├── emotion-cache-MIT
    │   ├── emotion-core-MIT
    │   ├── emotion-css-MIT
    │   ├── emotion-hash-MIT
    │   ├── emotion-is-prop-valid-MIT
    │   ├── emotion-memoize-MIT
    │   ├── emotion-serialize-MIT
    │   ├── emotion-sheet-MIT
    │   ├── emotion-stylis-MIT
    │   ├── emotion-unitless-MIT
    │   ├── emotion-utils-MIT
    │   ├── emotion-weak-memoize-MIT
    │   ├── encoding-MIT
    │   ├── error-ex-MIT
    │   ├── escalade-MIT
    │   ├── escape-string-regexp-MIT
    │   ├── evp_bytestokey-MIT
    │   ├── feelers-MIT
    │   ├── feelin-MIT
    │   ├── fill-range-MIT
    │   ├── find-root-MIT
    │   ├── find-up-MIT
    │   ├── flat-BSD-3-Clause
    │   ├── flatten-MIT
    │   ├── focus-trap-MIT
    │   ├── follow-redirects-MIT
    │   ├── form-data-MIT
    │   ├── fs.realpath-ISC
    │   ├── fsevents-MIT
    │   ├── function-bind-MIT
    │   ├── gensync-MIT
    │   ├── get-caller-file-ISC
    │   ├── glob-ISC
    │   ├── glob-parent-ISC
    │   ├── global-MIT
    │   ├── globals-MIT
    │   ├── growl-MIT
    │   ├── h2-MPL-2.0
    │   ├── hamcrest-BSD-3-Clause
    │   ├── hammerjs-MIT
    │   ├── has-flag-MIT
    │   ├── hash-base-MIT
    │   ├── hash.js-MIT
    │   ├── hasown-MIT
    │   ├── he-MIT
    │   ├── history-MIT
    │   ├── hmac-drbg-MIT
    │   ├── hoist-non-react-statics-BSD-3-Clause
    │   ├── iconv-lite-MIT
    │   ├── icu4j-Unicode
    │   ├── import-fresh-MIT
    │   ├── inflight-ISC
    │   ├── inherits-ISC
    │   ├── inherits-browser-ISC
    │   ├── invariant-MIT
    │   ├── is-arrayish-MIT
    │   ├── is-binary-path-MIT
    │   ├── is-core-module-MIT
    │   ├── is-extglob-MIT
    │   ├── is-fullwidth-code-point-MIT
    │   ├── is-glob-MIT
    │   ├── is-number-MIT
    │   ├── is-plain-obj-MIT
    │   ├── is-plain-object-MIT
    │   ├── is-what-MIT
    │   ├── isarray-MIT
    │   ├── isexe-ISC
    │   ├── isobject-MIT
    │   ├── isomorphic-fetch-MIT
    │   ├── janino-BSD-3-Clause
    │   ├── jedis-MIT
    │   ├── jquery-MIT
    │   ├── jridgewell-gen-mapping-MIT
    │   ├── jridgewell-resolve-uri-MIT
    │   ├── jridgewell-set-array-MIT
    │   ├── jridgewell-sourcemap-codec-MIT
    │   ├── jridgewell-trace-mapping-MIT
    │   ├── js-tokens-MIT
    │   ├── jsesc-MIT
    │   ├── json-parse-even-better-errors-MIT
    │   ├── json5-MIT
    │   ├── jul-to-slf4j-MIT
    │   ├── junit4-EPL-1.0
    │   ├── kryo-BSD-3-Clause
    │   ├── lang-feel-MIT
    │   ├── lezer-common-MIT
    │   ├── lezer-feel-MIT
    │   ├── lezer-highlight-MIT
    │   ├── lezer-lr-MIT
    │   ├── lezer-markdown-MIT
    │   ├── lines-and-columns-MIT
    │   ├── loader-utils-MIT
    │   ├── locate-path-MIT
    │   ├── lodash-MIT
    │   ├── lodash-es-MIT
    │   ├── lodash.clonedeep-MIT
    │   ├── log-symbols-MIT
    │   ├── loose-envify-MIT
    │   ├── lru-cache-ISC
    │   ├── luxon-MIT
    │   ├── md5.js-MIT
    │   ├── memoize-one-MIT
    │   ├── merge-anything-MIT
    │   ├── mime-db-MIT
    │   ├── mime-types-MIT
    │   ├── min-dash-MIT
    │   ├── min-document-MIT
    │   ├── min-dom-MIT
    │   ├── minimalistic-assert-ISC
    │   ├── minimalistic-crypto-utils-MIT
    │   ├── minimatch-ISC
    │   ├── minlog-BSD-3-Clause
    │   ├── mocha-MIT
    │   ├── moment-MIT
    │   ├── ms-MIT
    │   ├── mxparser-IUELSL
    │   ├── nanoid-MIT
    │   ├── node-fetch-MIT
    │   ├── node-releases-MIT
    │   ├── normalize-path-MIT
    │   ├── object-assign-MIT
    │   ├── object-refs-MIT
    │   ├── omit.js-MIT
    │   ├── once-ISC
    │   ├── p-limit-MIT
    │   ├── p-locate-MIT
    │   ├── parent-module-MIT
    │   ├── parse-asn1-ISC
    │   ├── parse-json-MIT
    │   ├── path-exists-MIT
    │   ├── path-intersection-MIT
    │   ├── path-is-absolute-MIT
    │   ├── path-parse-MIT
    │   ├── path-to-regexp-MIT
    │   ├── path-type-MIT
    │   ├── pbkdf2-MIT
    │   ├── picocolors-ISC
    │   ├── picomatch-MIT
    │   ├── postcss-value-parse-MIT
    │   ├── postcss-value-parser-MIT
    │   ├── postgresql-BSD-2-Clause
    │   ├── preact-MIT
    │   ├── process-MIT
    │   ├── process-nextick-args-MIT
    │   ├── prop-types-MIT
    │   ├── protobuf-java-BSD-3-Clause
    │   ├── proxy-from-env-MIT
    │   ├── randombytes-MIT
    │   ├── rax-BSD-3-Clause
    │   ├── react-MIT
    │   ├── react-dom-MIT
    │   ├── react-is-MIT
    │   ├── react-lifecycles-compat-MIT
    │   ├── react-loading-skeleton-MIT
    │   ├── react-redux-MIT
    │   ├── react-router-MIT
    │   ├── react-router-dom-MIT
    │   ├── react-router-redux-MIT
    │   ├── react-transition-group-BSD-3-Clause
    │   ├── readable-stream-MIT
    │   ├── readdirp-MIT
    │   ├── redux-MIT
    │   ├── redux-saga-MIT
    │   ├── redux-thunk-MIT
    │   ├── reflectasm-BSD-3-Clause
    │   ├── regenerator-runtime-MIT
    │   ├── require-directory-MIT
    │   ├── resize-observer-polyfill-MIT
    │   ├── resolve-MIT
    │   ├── resolve-from-MIT
    │   ├── resolve-pathname-MIT
    │   ├── ripemd160-MIT
    │   ├── safe-buffer-MIT
    │   ├── safer-buffer-MIT
    │   ├── scheduler-MIT
    │   ├── semver-ISC
    │   ├── serialize-javascript-BSD-3-Clause
    │   ├── sha.js-MIT
    │   ├── shallow-element-equals-MIT
    │   ├── slf4j-api-MIT
    │   ├── source-map-BSD-3-Clause
    │   ├── sprintf-js-BSD-3-Clause
    │   ├── ssr-window-MIT
    │   ├── string-decoder-MIT
    │   ├── string-width-MIT
    │   ├── string_decoder-MIT
    │   ├── strip-ansi-MIT
    │   ├── strip-json-comments-MIT
    │   ├── style-equal-MIT
    │   ├── style-mod-MIT
    │   ├── styled-components-MIT
    │   ├── stylis-MIT
    │   ├── stylis-rule-sheet-MIT
    │   ├── supports-color-MIT
    │   ├── supports-preserve-symlinks-flag-MIT
    │   ├── svelte-MIT
    │   ├── swiper-MIT
    │   ├── symbol-observable-MIT
    │   ├── tabbable-MIT
    │   ├── tiny-invariant-MIT
    │   ├── tiny-svg-MIT
    │   ├── tiny-warning-MIT
    │   ├── to-fast-properties-MIT
    │   ├── to-regex-range-MIT
    │   ├── tr46-MIT
    │   ├── tslib-OBSD
    │   ├── types-history-MIT
    │   ├── types-hoist-non-react-statics-MIT
    │   ├── types-isomorphic-fetch-MIT
    │   ├── types-parse-json-MIT
    │   ├── types-prop-types-MIT
    │   ├── types-react-MIT
    │   ├── types-react-dom-MIT
    │   ├── types-react-router-MIT
    │   ├── types-react-router-dom-MIT
    │   ├── types-react-router-redux-MIT
    │   ├── types-scheduler-MIT
    │   ├── types-use-sync-external-store-MIT
    │   ├── ungap-ISC
    │   ├── uni-BSD-3-Clause
    │   ├── universal-BSD-3-Clause
    │   ├── update-browserslist-db-MIT
    │   ├── use-sync-external-store-MIT
    │   ├── util-deprecate-MIT
    │   ├── value-equal-MIT
    │   ├── w3c-keyname-MIT
    │   ├── warning-BSD-3-Clause
    │   ├── warning-MIT
    │   ├── webidl-conversions-BSD-2-Clause
    │   ├── whatwg-fetch-MIT
    │   ├── whatwg-url-MIT
    │   ├── which-ISC
    │   ├── wide-align-ISC
    │   ├── wrap-ansi-MIT
    │   ├── wrappy-ISC
    │   ├── xstream-BSD-3-Clause
    │   ├── y18n-ISC
    │   ├── yallist-ISC
    │   ├── yaml-ISC
    │   ├── yamljs-MIT
    │   ├── yargs-MIT
    │   ├── yargs-parser-ISC
    │   ├── yargs-unparser-MIT
    │   ├── yocto-queue-MIT
    │   └── zstd-jni-BSD-2-Clause
    ├── script
    │   ├── config-center
    │   │   ├── README.md
    │   │   ├── apollo
    │   │   │   ├── apollo-config-interactive.sh
    │   │   │   └── apollo-config.sh
    │   │   ├── config.txt
    │   │   ├── consul
    │   │   │   ├── consul-config-interactive.sh
    │   │   │   └── consul-config.sh
    │   │   ├── etcd3
    │   │   │   ├── etcd3-config-interactive.sh
    │   │   │   └── etcd3-config.sh
    │   │   ├── nacos
    │   │   │   ├── nacos-config-interactive.py
    │   │   │   ├── nacos-config-interactive.sh
    │   │   │   ├── nacos-config.py
    │   │   │   └── nacos-config.sh
    │   │   └── zk
    │   │       ├── zk-config-interactive.sh
    │   │       └── zk-config.sh
    │   ├── logstash
    │   │   └── config
    │   │       ├── logstash-kafka.conf
    │   │       └── logstash-logback.conf
    │   └── server
    │       ├── db
    │       │   ├── dm.sql
    │       │   ├── kingbase.sql
    │       │   ├── mysql.sql
    │       │   ├── oracle.sql
    │       │   ├── oscar.sql
    │       │   ├── postgresql.sql
    │       │   └── sqlserver.sql
    │       ├── docker-compose
    │       │   └── docker-compose.yaml
    │       ├── helm
    │       │   └── seata-server
    │       │       ├── Chart.yaml
    │       │       ├── templates
    │       │       │   ├── NOTES.txt
    │       │       │   ├── _helpers.tpl
    │       │       │   ├── deployment.yaml
    │       │       │   ├── service.yaml
    │       │       │   └── tests
    │       │       │       └── test-connection.yaml
    │       │       └── values.yaml
    │       └── kubernetes
    │           └── seata-server.yaml
    └── target
        └── seata-server.jar

```

---

由于license兼容性问题，我们不能将mysql、mariadb、oracle等jar依赖包含在发布包中。
请将数据库driver相关依赖例如：`mysql-connector-java.jar`，拷贝到此目录下。目录结构示例如下：
```aidl
.
├── DISCLAIMER
├── seata-namingserver
│   ├── Dockerfile
│   ├── LICENSE
│   ├── NOTICE
│   ├── bin
│   │   ├── seata-namingserver-setup.sh
│   │   ├── seata-namingserver.bat
│   │   └── seata-namingserver.sh
│   ├── conf
│   │   ├── application.yml
│   │   ├── logback
│   │   │   ├── console-appender.xml
│   │   │   └── file-appender.xml
│   │   └── logback-spring.xml
│   ├── lib
│   │   ├── caffeine-2.9.3.jar
│   │   ├── checker-qual-3.37.0.jar
│   │   ├── commons-codec-1.15.jar
│   │   ├── commons-compiler-3.1.10.jar
│   │   ├── commons-io-2.8.0.jar
│   │   ├── commons-lang-2.6.jar
│   │   ├── commons-lang3-3.12.0.jar
│   │   ├── error_prone_annotations-2.21.1.jar
│   │   ├── httpasyncclient-4.1.5.jar
│   │   ├── httpclient-4.5.14.jar
│   │   ├── httpcore-4.4.16.jar
│   │   ├── httpcore-nio-4.4.16.jar
│   │   ├── jackson-annotations-2.13.5.jar
│   │   ├── jackson-core-2.13.5.jar
│   │   ├── jackson-databind-2.13.5.jar
│   │   ├── jackson-datatype-jdk8-2.13.5.jar
│   │   ├── jackson-datatype-jsr310-2.13.5.jar
│   │   ├── jackson-module-parameter-names-2.13.5.jar
│   │   ├── jakarta.annotation-api-1.3.5.jar
│   │   ├── janino-3.1.10.jar
│   │   ├── javax.servlet-api-4.0.1.jar
│   │   ├── jjwt-api-0.10.5.jar
│   │   ├── jjwt-impl-0.10.5.jar
│   │   ├── jjwt-jackson-0.10.5.jar
│   │   ├── jul-to-slf4j-1.7.36.jar
│   │   ├── logback-classic-1.2.12.jar
│   │   ├── logback-core-1.2.12.jar
│   │   ├── netty-all-4.1.101.Final.jar
│   │   ├── netty-buffer-4.1.101.Final.jar
│   │   ├── netty-codec-4.1.101.Final.jar
│   │   ├── netty-codec-dns-4.1.101.Final.jar
│   │   ├── netty-codec-haproxy-4.1.101.Final.jar
│   │   ├── netty-codec-http-4.1.101.Final.jar
│   │   ├── netty-codec-http2-4.1.101.Final.jar
│   │   ├── netty-codec-memcache-4.1.101.Final.jar
│   │   ├── netty-codec-mqtt-4.1.101.Final.jar
│   │   ├── netty-codec-redis-4.1.101.Final.jar
│   │   ├── netty-codec-smtp-4.1.101.Final.jar
│   │   ├── netty-codec-socks-4.1.101.Final.jar
│   │   ├── netty-codec-stomp-4.1.101.Final.jar
│   │   ├── netty-codec-xml-4.1.101.Final.jar
│   │   ├── netty-common-4.1.101.Final.jar
│   │   ├── netty-handler-4.1.101.Final.jar
│   │   ├── netty-handler-proxy-4.1.101.Final.jar
│   │   ├── netty-handler-ssl-ocsp-4.1.101.Final.jar
│   │   ├── netty-resolver-4.1.101.Final.jar
│   │   ├── netty-resolver-dns-4.1.101.Final.jar
│   │   ├── netty-resolver-dns-classes-macos-4.1.101.Final.jar
│   │   ├── netty-resolver-dns-native-macos-4.1.101.Final-osx-aarch_64.jar
│   │   ├── netty-resolver-dns-native-macos-4.1.101.Final-osx-x86_64.jar
│   │   ├── netty-transport-4.1.101.Final.jar
│   │   ├── netty-transport-classes-epoll-4.1.101.Final.jar
│   │   ├── netty-transport-classes-kqueue-4.1.101.Final.jar
│   │   ├── netty-transport-native-epoll-4.1.101.Final-linux-aarch_64.jar
│   │   ├── netty-transport-native-epoll-4.1.101.Final-linux-x86_64.jar
│   │   ├── netty-transport-native-kqueue-4.1.101.Final-osx-aarch_64.jar
│   │   ├── netty-transport-native-kqueue-4.1.101.Final-osx-x86_64.jar
│   │   ├── netty-transport-native-unix-common-4.1.101.Final.jar
│   │   ├── netty-transport-rxtx-4.1.101.Final.jar
│   │   ├── netty-transport-sctp-4.1.101.Final.jar
│   │   ├── netty-transport-udt-4.1.101.Final.jar
│   │   ├── seata-common-2.5.0.jar
│   │   ├── seata-console-2.5.0.jar
│   │   ├── slf4j-api-1.7.36.jar
│   │   ├── snakeyaml-2.0.jar
│   │   ├── spring-aop-5.3.39.jar
│   │   ├── spring-beans-5.3.39.jar
│   │   ├── spring-boot-2.7.18.jar
│   │   ├── spring-boot-autoconfigure-2.7.18.jar
│   │   ├── spring-boot-starter-2.7.18.jar
│   │   ├── spring-boot-starter-json-2.7.18.jar
│   │   ├── spring-boot-starter-logging-2.7.18.jar
│   │   ├── spring-boot-starter-security-2.7.18.jar
│   │   ├── spring-boot-starter-tomcat-2.7.18.jar
│   │   ├── spring-boot-starter-web-2.7.18.jar
│   │   ├── spring-context-5.3.39.jar
│   │   ├── spring-core-5.3.39.jar
│   │   ├── spring-expression-5.3.39.jar
│   │   ├── spring-jcl-5.3.39.jar
│   │   ├── spring-security-config-5.7.11.jar
│   │   ├── spring-security-core-5.7.11.jar
│   │   ├── spring-security-crypto-5.7.11.jar
│   │   ├── spring-security-web-5.7.11.jar
│   │   ├── spring-web-5.3.39.jar
│   │   ├── spring-webmvc-5.3.39.jar
│   │   ├── tomcat-annotations-api-9.0.83.jar
│   │   ├── tomcat-embed-core-9.0.106.jar
│   │   ├── tomcat-embed-el-9.0.106.jar
│   │   └── tomcat-embed-websocket-9.0.106.jar
│   ├── licenses
│   │   ├── Apache-1.1
│   │   ├── CDDL+GPL-1.1
│   │   ├── CDDL-1.0
│   │   ├── EPL-1.0
│   │   ├── EPL-2.0
│   │   ├── Python-2.0
│   │   ├── abego-treelayout-BSD-3-Clause
│   │   ├── alicloud-console-components-MIT
│   │   ├── alicloud-console-components-actions-MIT
│   │   ├── alifd-field-MIT
│   │   ├── alifd-next-MIT
│   │   ├── alifd-validate-MIT
│   │   ├── ansi-colors-MIT
│   │   ├── ansi-regex-MIT
│   │   ├── ansi-styles-MIT
│   │   ├── antlr-stringtemplate3-BSD-3-Clause
│   │   ├── antlr2-BSD-3-Clause
│   │   ├── antlr3-BSD
│   │   ├── antlr4-BSD
│   │   ├── antlr4-ST4-BSD
│   │   ├── anymatch-ISC
│   │   ├── argparse-MIT
│   │   ├── asm-BSD-3-Clause
│   │   ├── asn1.js-MIT
│   │   ├── asynckit-MIT
│   │   ├── axios-MIT
│   │   ├── babel-code-frame-MIT
│   │   ├── babel-compat-data-MIT
│   │   ├── babel-core-MIT
│   │   ├── babel-generator-MIT
│   │   ├── babel-helper-annotate-as-pure-MIT
│   │   ├── babel-helper-compilation-targets-MIT
│   │   ├── babel-helper-environment-visitor-MIT
│   │   ├── babel-helper-function-name-MIT
│   │   ├── babel-helper-hoist-variables-MIT
│   │   ├── babel-helper-module-imports-MIT
│   │   ├── babel-helper-module-transforms-MIT
│   │   ├── babel-helper-plugin-utils-MIT
│   │   ├── babel-helper-simple-access-MIT
│   │   ├── babel-helper-split-export-declaration-MIT
│   │   ├── babel-helper-string-parser-MIT
│   │   ├── babel-helper-validator-identifier-MIT
│   │   ├── babel-helper-validator-option-MIT
│   │   ├── babel-helpers-MIT
│   │   ├── babel-highlight-MIT
│   │   ├── babel-parser-MIT
│   │   ├── babel-plugin-emotion-MIT
│   │   ├── babel-plugin-macros-MIT
│   │   ├── babel-plugin-styled-components-MIT
│   │   ├── babel-plugin-syntax-jsx-MIT
│   │   ├── babel-runtime-MIT
│   │   ├── babel-template-MIT
│   │   ├── babel-traverse-MIT
│   │   ├── babel-types-MIT
│   │   ├── balanced-match-MIT
│   │   ├── bignumber.js-MIT
│   │   ├── bn.js-MIT
│   │   ├── bpmn-font-SIL
│   │   ├── bpmn-io-cm-theme-MIT
│   │   ├── bpmn-io-diagram-js-ui-MIT
│   │   ├── bpmn-io-feel-editor-MIT
│   │   ├── bpmn-io-feel-lint-MIT
│   │   ├── bpmn-io-properties-panel-MIT
│   │   ├── brace-expansion-MIT
│   │   ├── braces-2.3.1-MIT
│   │   ├── braces-3.0.2-MIT
│   │   ├── braces-MIT
│   │   ├── brorand-MIT
│   │   ├── browser-stdout-ISC
│   │   ├── browserify-aes-MIT
│   │   ├── browserify-rsa-MIT
│   │   ├── browserify-sign-ISC
│   │   ├── browserslist-MIT
│   │   ├── buffer-xor-MIT
│   │   ├── callsites-MIT
│   │   ├── camelcase-MIT
│   │   ├── camelize-MIT
│   │   ├── chalk-MIT
│   │   ├── checker-qual-MIT
│   │   ├── chokidar-MIT
│   │   ├── cipher-base-MIT
│   │   ├── classnames-2.5.1-MIT
│   │   ├── classnames-MIT
│   │   ├── cliui-ISC
│   │   ├── clsx-MIT
│   │   ├── codemirror-autocomplete-MIT
│   │   ├── codemirror-commands-MIT
│   │   ├── codemirror-language-MIT
│   │   ├── codemirror-lint-MIT
│   │   ├── codemirror-state-MIT
│   │   ├── codemirror-view-MIT
│   │   ├── color-convert-MIT
│   │   ├── color-name-MIT
│   │   ├── combined-stream-MIT
│   │   ├── component-event-MIT
│   │   ├── concat-map-MIT
│   │   ├── convert-source-map-MIT
│   │   ├── core-js-MIT
│   │   ├── core-util-is-MIT
│   │   ├── cosmiconfig-MIT
│   │   ├── create-hash-MIT
│   │   ├── create-hmac-MIT
│   │   ├── crelt-MIT
│   │   ├── css-color-keywords-ISC
│   │   ├── css-to-react-native-MIT
│   │   ├── csstype-MIT
│   │   ├── dayjs-MIT
│   │   ├── debug-MIT
│   │   ├── decamelize-MIT
│   │   ├── decode-uri-component-MIT
│   │   ├── delayed-stream-MIT
│   │   ├── dexx-collections-MIT
│   │   ├── diagram-js-MIT
│   │   ├── diagram-js-grid-MIT
│   │   ├── didi-MIT
│   │   ├── dom-helpers-MIT
│   │   ├── dom-walk-MIT
│   │   ├── dom7-MIT
│   │   ├── domify-MIT
│   │   ├── driver-dom-BSD-3-Clause
│   │   ├── driver-miniapp-BSD-3-Clause
│   │   ├── driver-universal-BSD-3-Clause
│   │   ├── driver-weex-BSD-3-Clause
│   │   ├── dva-MIT
│   │   ├── dva-core-MIT
│   │   ├── electron-to-chromium-ISC
│   │   ├── elliptic-MIT
│   │   ├── emotion-cache-MIT
│   │   ├── emotion-core-MIT
│   │   ├── emotion-css-MIT
│   │   ├── emotion-hash-MIT
│   │   ├── emotion-is-prop-valid-MIT
│   │   ├── emotion-memoize-MIT
│   │   ├── emotion-serialize-MIT
│   │   ├── emotion-sheet-MIT
│   │   ├── emotion-stylis-MIT
│   │   ├── emotion-unitless-MIT
│   │   ├── emotion-utils-MIT
│   │   ├── emotion-weak-memoize-MIT
│   │   ├── encoding-MIT
│   │   ├── error-ex-MIT
│   │   ├── escalade-MIT
│   │   ├── escape-string-regexp-MIT
│   │   ├── evp_bytestokey-MIT
│   │   ├── feelers-MIT
│   │   ├── feelin-MIT
│   │   ├── fill-range-MIT
│   │   ├── find-root-MIT
│   │   ├── find-up-MIT
│   │   ├── flat-BSD-3-Clause
│   │   ├── flatten-MIT
│   │   ├── focus-trap-MIT
│   │   ├── follow-redirects-MIT
│   │   ├── form-data-MIT
│   │   ├── fs.realpath-ISC
│   │   ├── fsevents-MIT
│   │   ├── function-bind-MIT
│   │   ├── gensync-MIT
│   │   ├── get-caller-file-ISC
│   │   ├── glob-ISC
│   │   ├── glob-parent-ISC
│   │   ├── global-MIT
│   │   ├── globals-MIT
│   │   ├── growl-MIT
│   │   ├── h2-MPL-2.0
│   │   ├── hamcrest-BSD-3-Clause
│   │   ├── hammerjs-MIT
│   │   ├── has-flag-MIT
│   │   ├── hash-base-MIT
│   │   ├── hash.js-MIT
│   │   ├── hasown-MIT
│   │   ├── he-MIT
│   │   ├── history-MIT
│   │   ├── hmac-drbg-MIT
│   │   ├── hoist-non-react-statics-BSD-3-Clause
│   │   ├── iconv-lite-MIT
│   │   ├── icu4j-Unicode
│   │   ├── import-fresh-MIT
│   │   ├── inflight-ISC
│   │   ├── inherits-ISC
│   │   ├── inherits-browser-ISC
│   │   ├── invariant-MIT
│   │   ├── is-arrayish-MIT
│   │   ├── is-binary-path-MIT
│   │   ├── is-core-module-MIT
│   │   ├── is-extglob-MIT
│   │   ├── is-fullwidth-code-point-MIT
│   │   ├── is-glob-MIT
│   │   ├── is-number-MIT
│   │   ├── is-plain-obj-MIT
│   │   ├── is-plain-object-MIT
│   │   ├── is-what-MIT
│   │   ├── isarray-MIT
│   │   ├── isexe-ISC
│   │   ├── isobject-MIT
│   │   ├── isomorphic-fetch-MIT
│   │   ├── janino-BSD-3-Clause
│   │   ├── jedis-MIT
│   │   ├── jquery-MIT
│   │   ├── jridgewell-gen-mapping-MIT
│   │   ├── jridgewell-resolve-uri-MIT
│   │   ├── jridgewell-set-array-MIT
│   │   ├── jridgewell-sourcemap-codec-MIT
│   │   ├── jridgewell-trace-mapping-MIT
│   │   ├── js-tokens-MIT
│   │   ├── jsesc-MIT
│   │   ├── json-parse-even-better-errors-MIT
│   │   ├── json5-MIT
│   │   ├── jul-to-slf4j-MIT
│   │   ├── junit4-EPL-1.0
│   │   ├── kryo-BSD-3-Clause
│   │   ├── lang-feel-MIT
│   │   ├── lezer-common-MIT
│   │   ├── lezer-feel-MIT
│   │   ├── lezer-highlight-MIT
│   │   ├── lezer-lr-MIT
│   │   ├── lezer-markdown-MIT
│   │   ├── lines-and-columns-MIT
│   │   ├── loader-utils-MIT
│   │   ├── locate-path-MIT
│   │   ├── lodash-MIT
│   │   ├── lodash-es-MIT
│   │   ├── lodash.clonedeep-MIT
│   │   ├── log-symbols-MIT
│   │   ├── loose-envify-MIT
│   │   ├── lru-cache-ISC
│   │   ├── luxon-MIT
│   │   ├── md5.js-MIT
│   │   ├── memoize-one-MIT
│   │   ├── merge-anything-MIT
│   │   ├── mime-db-MIT
│   │   ├── mime-types-MIT
│   │   ├── min-dash-MIT
│   │   ├── min-document-MIT
│   │   ├── min-dom-MIT
│   │   ├── minimalistic-assert-ISC
│   │   ├── minimalistic-crypto-utils-MIT
│   │   ├── minimatch-ISC
│   │   ├── minlog-BSD-3-Clause
│   │   ├── mocha-MIT
│   │   ├── moment-MIT
│   │   ├── ms-MIT
│   │   ├── mxparser-IUELSL
│   │   ├── nanoid-MIT
│   │   ├── node-fetch-MIT
│   │   ├── node-releases-MIT
│   │   ├── normalize-path-MIT
│   │   ├── object-assign-MIT
│   │   ├── object-refs-MIT
│   │   ├── omit.js-MIT
│   │   ├── once-ISC
│   │   ├── p-limit-MIT
│   │   ├── p-locate-MIT
│   │   ├── parent-module-MIT
│   │   ├── parse-asn1-ISC
│   │   ├── parse-json-MIT
│   │   ├── path-exists-MIT
│   │   ├── path-intersection-MIT
│   │   ├── path-is-absolute-MIT
│   │   ├── path-parse-MIT
│   │   ├── path-to-regexp-MIT
│   │   ├── path-type-MIT
│   │   ├── pbkdf2-MIT
│   │   ├── picocolors-ISC
│   │   ├── picomatch-MIT
│   │   ├── postcss-value-parse-MIT
│   │   ├── postcss-value-parser-MIT
│   │   ├── postgresql-BSD-2-Clause
│   │   ├── preact-MIT
│   │   ├── process-MIT
│   │   ├── process-nextick-args-MIT
│   │   ├── prop-types-MIT
│   │   ├── protobuf-java-BSD-3-Clause
│   │   ├── proxy-from-env-MIT
│   │   ├── randombytes-MIT
│   │   ├── rax-BSD-3-Clause
│   │   ├── react-MIT
│   │   ├── react-dom-MIT
│   │   ├── react-is-MIT
│   │   ├── react-lifecycles-compat-MIT
│   │   ├── react-loading-skeleton-MIT
│   │   ├── react-redux-MIT
│   │   ├── react-router-MIT
│   │   ├── react-router-dom-MIT
│   │   ├── react-router-redux-MIT
│   │   ├── react-transition-group-BSD-3-Clause
│   │   ├── readable-stream-MIT
│   │   ├── readdirp-MIT
│   │   ├── redux-MIT
│   │   ├── redux-saga-MIT
│   │   ├── redux-thunk-MIT
│   │   ├── reflectasm-BSD-3-Clause
│   │   ├── regenerator-runtime-MIT
│   │   ├── require-directory-MIT
│   │   ├── resize-observer-polyfill-MIT
│   │   ├── resolve-MIT
│   │   ├── resolve-from-MIT
│   │   ├── resolve-pathname-MIT
│   │   ├── ripemd160-MIT
│   │   ├── safe-buffer-MIT
│   │   ├── safer-buffer-MIT
│   │   ├── scheduler-MIT
│   │   ├── semver-ISC
│   │   ├── serialize-javascript-BSD-3-Clause
│   │   ├── sha.js-MIT
│   │   ├── shallow-element-equals-MIT
│   │   ├── slf4j-api-MIT
│   │   ├── source-map-BSD-3-Clause
│   │   ├── sprintf-js-BSD-3-Clause
│   │   ├── ssr-window-MIT
│   │   ├── string-decoder-MIT
│   │   ├── string-width-MIT
│   │   ├── string_decoder-MIT
│   │   ├── strip-ansi-MIT
│   │   ├── strip-json-comments-MIT
│   │   ├── style-equal-MIT
│   │   ├── style-mod-MIT
│   │   ├── styled-components-MIT
│   │   ├── stylis-MIT
│   │   ├── stylis-rule-sheet-MIT
│   │   ├── supports-color-MIT
│   │   ├── supports-preserve-symlinks-flag-MIT
│   │   ├── svelte-MIT
│   │   ├── swiper-MIT
│   │   ├── symbol-observable-MIT
│   │   ├── tabbable-MIT
│   │   ├── tiny-invariant-MIT
│   │   ├── tiny-svg-MIT
│   │   ├── tiny-warning-MIT
│   │   ├── to-fast-properties-MIT
│   │   ├── to-regex-range-MIT
│   │   ├── tr46-MIT
│   │   ├── tslib-OBSD
│   │   ├── types-history-MIT
│   │   ├── types-hoist-non-react-statics-MIT
│   │   ├── types-isomorphic-fetch-MIT
│   │   ├── types-parse-json-MIT
│   │   ├── types-prop-types-MIT
│   │   ├── types-react-MIT
│   │   ├── types-react-dom-MIT
│   │   ├── types-react-router-MIT
│   │   ├── types-react-router-dom-MIT
│   │   ├── types-react-router-redux-MIT
│   │   ├── types-scheduler-MIT
│   │   ├── types-use-sync-external-store-MIT
│   │   ├── ungap-ISC
│   │   ├── uni-BSD-3-Clause
│   │   ├── universal-BSD-3-Clause
│   │   ├── update-browserslist-db-MIT
│   │   ├── use-sync-external-store-MIT
│   │   ├── util-deprecate-MIT
│   │   ├── value-equal-MIT
│   │   ├── w3c-keyname-MIT
│   │   ├── warning-BSD-3-Clause
│   │   ├── warning-MIT
│   │   ├── webidl-conversions-BSD-2-Clause
│   │   ├── whatwg-fetch-MIT
│   │   ├── whatwg-url-MIT
│   │   ├── which-ISC
│   │   ├── wide-align-ISC
│   │   ├── wrap-ansi-MIT
│   │   ├── wrappy-ISC
│   │   ├── xstream-BSD-3-Clause
│   │   ├── y18n-ISC
│   │   ├── yallist-ISC
│   │   ├── yaml-ISC
│   │   ├── yamljs-MIT
│   │   ├── yargs-MIT
│   │   ├── yargs-parser-ISC
│   │   ├── yargs-unparser-MIT
│   │   ├── yocto-queue-MIT
│   │   └── zstd-jni-BSD-2-Clause
│   └── target
│       └── seata-namingserver.jar
└── seata-server
    ├── Dockerfile
    ├── LICENSE
    ├── NOTICE
    ├── bin
    │   ├── seata-server.bat
    │   ├── seata-server.sh
    │   └── seata-setup.sh
    ├── conf
    │   ├── application.example.yml
    │   ├── application.raft.example.yml
    │   ├── application.yml
    │   ├── logback
    │   │   ├── console-appender.xml
    │   │   ├── file-appender.xml
    │   │   ├── kafka-appender.xml
    │   │   ├── logstash-appender.xml
    │   │   └── metric-appender.xml
    │   └── logback-spring.xml
    ├── ext
    │   └── apm-skywalking
    │       ├── plugins
    │       │   ├── apm-jdbc-commons-8.6.0.jar
    │       │   ├── apm-mysql-5.x-plugin-8.6.0.jar
    │       │   ├── apm-mysql-6.x-plugin-8.6.0.jar
    │       │   ├── apm-mysql-8.x-plugin-8.6.0.jar
    │       │   ├── apm-mysql-commons-8.6.0.jar
    │       │   └── apm-seata-skywalking-plugin-2.5.0.jar
    │       └── skywalking-agent.jar
    ├── lib
    │   ├── DmJdbcDriver18-8.1.2.192.jar
    │   ├── HikariCP-4.0.3.jar
    │   ├── ant-1.10.12.jar
    │   ├── ant-launcher-1.10.12.jar
    │   ├── aopalliance-1.0.jar
    │   ├── apollo-client-2.0.1.jar
    │   ├── apollo-core-2.0.1.jar
    │   ├── archaius-core-0.7.6.jar
    │   ├── asm-6.0.jar
    │   ├── audience-annotations-0.12.0.jar
    │   ├── bolt-1.6.7.jar
    │   ├── bucket4j_jdk8-core-8.1.0.jar
    │   ├── checker-qual-3.37.0.jar
    │   ├── commons-codec-1.15.jar
    │   ├── commons-compiler-3.1.10.jar
    │   ├── commons-configuration-1.10.jar
    │   ├── commons-dbcp2-2.9.0.jar
    │   ├── commons-io-2.8.0.jar
    │   ├── commons-jxpath-1.3.jar
    │   ├── commons-lang-2.6.jar
    │   ├── commons-logging-1.2.jar
    │   ├── commons-math-2.2.jar
    │   ├── commons-pool-1.6.jar
    │   ├── commons-pool2-2.11.1.jar
    │   ├── compactmap-2.0.jar
    │   ├── config-1.2.1.jar
    │   ├── consul-api-1.4.2.jar
    │   ├── curator-client-5.1.0.jar
    │   ├── curator-framework-5.1.0.jar
    │   ├── curator-recipes-5.1.0.jar
    │   ├── curator-test-5.1.0.jar
    │   ├── dexx-collections-0.2.jar
    │   ├── disruptor-3.3.7.jar
    │   ├── druid-1.2.20.jar
    │   ├── error_prone_annotations-2.21.1.jar
    │   ├── eureka-client-1.10.18.jar
    │   ├── failsafe-2.3.3.jar
    │   ├── failureaccess-1.0.1.jar
    │   ├── fastjson-1.2.83.jar
    │   ├── fastjson2-2.0.52.jar
    │   ├── fury-core-0.8.0.jar
    │   ├── grpc-api-1.55.1.jar
    │   ├── grpc-context-1.55.1.jar
    │   ├── grpc-grpclb-1.27.1.jar
    │   ├── grpc-netty-1.55.1.jar
    │   ├── grpc-protobuf-1.55.1.jar
    │   ├── grpc-protobuf-lite-1.55.1.jar
    │   ├── grpc-stub-1.55.1.jar
    │   ├── gson-2.9.1.jar
    │   ├── guava-32.1.3-jre.jar
    │   ├── guice-5.0.1.jar
    │   ├── hamcrest-2.2.jar
    │   ├── hamcrest-core-2.2.jar
    │   ├── hessian-4.0.3.jar
    │   ├── hessian-4.0.63.jar
    │   ├── httpasyncclient-4.1.5.jar
    │   ├── httpclient-4.5.14.jar
    │   ├── httpcore-4.4.16.jar
    │   ├── httpcore-nio-4.4.16.jar
    │   ├── j2objc-annotations-2.8.jar
    │   ├── jackson-annotations-2.13.5.jar
    │   ├── jackson-core-2.13.5.jar
    │   ├── jackson-databind-2.13.5.jar
    │   ├── jakarta.annotation-api-1.3.5.jar
    │   ├── janino-3.1.10.jar
    │   ├── javax.inject-1.jar
    │   ├── javax.servlet-api-4.0.1.jar
    │   ├── jcommander-1.82.jar
    │   ├── jctools-core-2.1.1.jar
    │   ├── jdbc
    │   │   └── NOTICE.md
    │   ├── jedis-3.8.0.jar
    │   ├── jersey-apache-client4-1.19.1.jar
    │   ├── jersey-client-1.19.1.jar
    │   ├── jersey-core-1.19.1.jar
    │   ├── jetcd-common-0.5.0.jar
    │   ├── jetcd-core-0.5.0.jar
    │   ├── jetcd-resolver-0.5.0.jar
    │   ├── jettison-1.5.4.jar
    │   ├── jna-5.5.0.jar
    │   ├── joda-time-2.3.jar
    │   ├── jraft-core-1.3.14.jar
    │   ├── jsr305-3.0.2.jar
    │   ├── jsr311-api-1.1.1.jar
    │   ├── jul-to-slf4j-1.7.36.jar
    │   ├── junit-4.13.2.jar
    │   ├── kafka-clients-3.6.1.jar
    │   ├── kryo-5.4.0.jar
    │   ├── kryo-serializers-0.45.jar
    │   ├── logback-classic-1.2.12.jar
    │   ├── logback-core-1.2.12.jar
    │   ├── logback-kafka-appender-0.2.0-RC2.jar
    │   ├── logstash-logback-encoder-6.5.jar
    │   ├── lz4-java-1.7.1.jar
    │   ├── metrics-core-4.2.22.jar
    │   ├── minlog-1.3.1.jar
    │   ├── mxparser-1.2.2.jar
    │   ├── nacos-api-1.4.6.jar
    │   ├── nacos-client-1.4.6.jar
    │   ├── nacos-common-1.4.6.jar
    │   ├── netflix-eventbus-0.3.0.jar
    │   ├── netflix-infix-0.3.0.jar
    │   ├── netty-all-4.1.101.Final.jar
    │   ├── netty-buffer-4.1.101.Final.jar
    │   ├── netty-codec-4.1.101.Final.jar
    │   ├── netty-codec-dns-4.1.101.Final.jar
    │   ├── netty-codec-haproxy-4.1.101.Final.jar
    │   ├── netty-codec-http-4.1.101.Final.jar
    │   ├── netty-codec-http2-4.1.101.Final.jar
    │   ├── netty-codec-memcache-4.1.101.Final.jar
    │   ├── netty-codec-mqtt-4.1.101.Final.jar
    │   ├── netty-codec-redis-4.1.101.Final.jar
    │   ├── netty-codec-smtp-4.1.101.Final.jar
    │   ├── netty-codec-socks-4.1.101.Final.jar
    │   ├── netty-codec-stomp-4.1.101.Final.jar
    │   ├── netty-codec-xml-4.1.101.Final.jar
    │   ├── netty-common-4.1.101.Final.jar
    │   ├── netty-handler-4.1.101.Final.jar
    │   ├── netty-handler-proxy-4.1.101.Final.jar
    │   ├── netty-handler-ssl-ocsp-4.1.101.Final.jar
    │   ├── netty-resolver-4.1.101.Final.jar
    │   ├── netty-resolver-dns-4.1.101.Final.jar
    │   ├── netty-resolver-dns-classes-macos-4.1.101.Final.jar
    │   ├── netty-resolver-dns-native-macos-4.1.101.Final-osx-aarch_64.jar
    │   ├── netty-resolver-dns-native-macos-4.1.101.Final-osx-x86_64.jar
    │   ├── netty-transport-4.1.101.Final.jar
    │   ├── netty-transport-classes-epoll-4.1.101.Final.jar
    │   ├── netty-transport-classes-kqueue-4.1.101.Final.jar
    │   ├── netty-transport-native-epoll-4.1.101.Final-linux-aarch_64.jar
    │   ├── netty-transport-native-epoll-4.1.101.Final-linux-x86_64.jar
    │   ├── netty-transport-native-kqueue-4.1.101.Final-osx-aarch_64.jar
    │   ├── netty-transport-native-kqueue-4.1.101.Final-osx-x86_64.jar
    │   ├── netty-transport-native-unix-common-4.1.101.Final.jar
    │   ├── netty-transport-rxtx-4.1.101.Final.jar
    │   ├── netty-transport-sctp-4.1.101.Final.jar
    │   ├── netty-transport-udt-4.1.101.Final.jar
    │   ├── objenesis-3.2.jar
    │   ├── perfmark-api-0.25.0.jar
    │   ├── postgresql-42.3.8.jar
    │   ├── proto-google-common-protos-2.9.0.jar
    │   ├── protobuf-java-3.25.5.jar
    │   ├── protobuf-java-util-3.11.0.jar
    │   ├── reflectasm-1.11.9.jar
    │   ├── registry-client-all-6.3.0.jar
    │   ├── rocksdbjni-8.8.1.jar
    │   ├── seata-common-2.5.0.jar
    │   ├── seata-compressor-all-2.5.0.jar
    │   ├── seata-compressor-bzip2-2.5.0.jar
    │   ├── seata-compressor-deflater-2.5.0.jar
    │   ├── seata-compressor-gzip-2.5.0.jar
    │   ├── seata-compressor-lz4-2.5.0.jar
    │   ├── seata-compressor-zip-2.5.0.jar
    │   ├── seata-compressor-zstd-2.5.0.jar
    │   ├── seata-config-all-2.5.0.jar
    │   ├── seata-config-apollo-2.5.0.jar
    │   ├── seata-config-consul-2.5.0.jar
    │   ├── seata-config-core-2.5.0.jar
    │   ├── seata-config-etcd3-2.5.0.jar
    │   ├── seata-config-nacos-2.5.0.jar
    │   ├── seata-config-spring-cloud-2.5.0.jar
    │   ├── seata-config-zk-2.5.0.jar
    │   ├── seata-core-2.5.0.jar
    │   ├── seata-discovery-all-2.5.0.jar
    │   ├── seata-discovery-consul-2.5.0.jar
    │   ├── seata-discovery-core-2.5.0.jar
    │   ├── seata-discovery-custom-2.5.0.jar
    │   ├── seata-discovery-etcd3-2.5.0.jar
    │   ├── seata-discovery-eureka-2.5.0.jar
    │   ├── seata-discovery-nacos-2.5.0.jar
    │   ├── seata-discovery-namingserver-2.5.0.jar
    │   ├── seata-discovery-redis-2.5.0.jar
    │   ├── seata-discovery-sofa-2.5.0.jar
    │   ├── seata-discovery-zk-2.5.0.jar
    │   ├── seata-metrics-all-2.5.0.jar
    │   ├── seata-metrics-api-2.5.0.jar
    │   ├── seata-metrics-core-2.5.0.jar
    │   ├── seata-metrics-exporter-prometheus-2.5.0.jar
    │   ├── seata-metrics-registry-compact-2.5.0.jar
    │   ├── seata-serializer-all-2.5.0.jar
    │   ├── seata-serializer-fastjson2-2.5.0.jar
    │   ├── seata-serializer-fury-2.5.0.jar
    │   ├── seata-serializer-hessian-2.5.0.jar
    │   ├── seata-serializer-kryo-2.5.0.jar
    │   ├── seata-serializer-protobuf-2.5.0.jar
    │   ├── seata-serializer-seata-2.5.0.jar
    │   ├── seata-spring-autoconfigure-core-2.5.0.jar
    │   ├── seata-spring-autoconfigure-server-2.5.0.jar
    │   ├── servo-core-0.12.21.jar
    │   ├── simpleclient-0.15.0.jar
    │   ├── simpleclient_common-0.15.0.jar
    │   ├── simpleclient_httpserver-0.15.0.jar
    │   ├── simpleclient_tracer_common-0.15.0.jar
    │   ├── simpleclient_tracer_otel-0.15.0.jar
    │   ├── simpleclient_tracer_otel_agent-0.15.0.jar
    │   ├── slf4j-api-1.7.36.jar
    │   ├── snakeyaml-2.0.jar
    │   ├── snappy-java-1.1.10.5.jar
    │   ├── sofa-common-tools-1.0.12.jar
    │   ├── spring-aop-5.3.39.jar
    │   ├── spring-beans-5.3.39.jar
    │   ├── spring-boot-2.7.18.jar
    │   ├── spring-boot-autoconfigure-2.7.18.jar
    │   ├── spring-boot-starter-2.7.18.jar
    │   ├── spring-boot-starter-logging-2.7.18.jar
    │   ├── spring-context-5.3.39.jar
    │   ├── spring-core-5.3.39.jar
    │   ├── spring-expression-5.3.39.jar
    │   ├── spring-jcl-5.3.39.jar
    │   ├── spring-test-5.3.39.jar
    │   ├── spring-web-5.3.39.jar
    │   ├── xstream-1.4.21.jar
    │   ├── zookeeper-3.7.2.jar
    │   ├── zookeeper-jute-3.7.2.jar
    │   └── zstd-jni-1.5.0-4.jar
    ├── licenses
    │   ├── Apache-1.1
    │   ├── CDDL+GPL-1.1
    │   ├── CDDL-1.0
    │   ├── EPL-1.0
    │   ├── EPL-2.0
    │   ├── Python-2.0
    │   ├── abego-treelayout-BSD-3-Clause
    │   ├── alicloud-console-components-MIT
    │   ├── alicloud-console-components-actions-MIT
    │   ├── alifd-field-MIT
    │   ├── alifd-next-MIT
    │   ├── alifd-validate-MIT
    │   ├── ansi-colors-MIT
    │   ├── ansi-regex-MIT
    │   ├── ansi-styles-MIT
    │   ├── antlr-stringtemplate3-BSD-3-Clause
    │   ├── antlr2-BSD-3-Clause
    │   ├── antlr3-BSD
    │   ├── antlr4-BSD
    │   ├── antlr4-ST4-BSD
    │   ├── anymatch-ISC
    │   ├── argparse-MIT
    │   ├── asm-BSD-3-Clause
    │   ├── asn1.js-MIT
    │   ├── asynckit-MIT
    │   ├── axios-MIT
    │   ├── babel-code-frame-MIT
    │   ├── babel-compat-data-MIT
    │   ├── babel-core-MIT
    │   ├── babel-generator-MIT
    │   ├── babel-helper-annotate-as-pure-MIT
    │   ├── babel-helper-compilation-targets-MIT
    │   ├── babel-helper-environment-visitor-MIT
    │   ├── babel-helper-function-name-MIT
    │   ├── babel-helper-hoist-variables-MIT
    │   ├── babel-helper-module-imports-MIT
    │   ├── babel-helper-module-transforms-MIT
    │   ├── babel-helper-plugin-utils-MIT
    │   ├── babel-helper-simple-access-MIT
    │   ├── babel-helper-split-export-declaration-MIT
    │   ├── babel-helper-string-parser-MIT
    │   ├── babel-helper-validator-identifier-MIT
    │   ├── babel-helper-validator-option-MIT
    │   ├── babel-helpers-MIT
    │   ├── babel-highlight-MIT
    │   ├── babel-parser-MIT
    │   ├── babel-plugin-emotion-MIT
    │   ├── babel-plugin-macros-MIT
    │   ├── babel-plugin-styled-components-MIT
    │   ├── babel-plugin-syntax-jsx-MIT
    │   ├── babel-runtime-MIT
    │   ├── babel-template-MIT
    │   ├── babel-traverse-MIT
    │   ├── babel-types-MIT
    │   ├── balanced-match-MIT
    │   ├── bignumber.js-MIT
    │   ├── bn.js-MIT
    │   ├── bpmn-font-SIL
    │   ├── bpmn-io-cm-theme-MIT
    │   ├── bpmn-io-diagram-js-ui-MIT
    │   ├── bpmn-io-feel-editor-MIT
    │   ├── bpmn-io-feel-lint-MIT
    │   ├── bpmn-io-properties-panel-MIT
    │   ├── brace-expansion-MIT
    │   ├── braces-2.3.1-MIT
    │   ├── braces-3.0.2-MIT
    │   ├── braces-MIT
    │   ├── brorand-MIT
    │   ├── browser-stdout-ISC
    │   ├── browserify-aes-MIT
    │   ├── browserify-rsa-MIT
    │   ├── browserify-sign-ISC
    │   ├── browserslist-MIT
    │   ├── buffer-xor-MIT
    │   ├── callsites-MIT
    │   ├── camelcase-MIT
    │   ├── camelize-MIT
    │   ├── chalk-MIT
    │   ├── checker-qual-MIT
    │   ├── chokidar-MIT
    │   ├── cipher-base-MIT
    │   ├── classnames-2.5.1-MIT
    │   ├── classnames-MIT
    │   ├── cliui-ISC
    │   ├── clsx-MIT
    │   ├── codemirror-autocomplete-MIT
    │   ├── codemirror-commands-MIT
    │   ├── codemirror-language-MIT
    │   ├── codemirror-lint-MIT
    │   ├── codemirror-state-MIT
    │   ├── codemirror-view-MIT
    │   ├── color-convert-MIT
    │   ├── color-name-MIT
    │   ├── combined-stream-MIT
    │   ├── component-event-MIT
    │   ├── concat-map-MIT
    │   ├── convert-source-map-MIT
    │   ├── core-js-MIT
    │   ├── core-util-is-MIT
    │   ├── cosmiconfig-MIT
    │   ├── create-hash-MIT
    │   ├── create-hmac-MIT
    │   ├── crelt-MIT
    │   ├── css-color-keywords-ISC
    │   ├── css-to-react-native-MIT
    │   ├── csstype-MIT
    │   ├── dayjs-MIT
    │   ├── debug-MIT
    │   ├── decamelize-MIT
    │   ├── decode-uri-component-MIT
    │   ├── delayed-stream-MIT
    │   ├── dexx-collections-MIT
    │   ├── diagram-js-MIT
    │   ├── diagram-js-grid-MIT
    │   ├── didi-MIT
    │   ├── dom-helpers-MIT
    │   ├── dom-walk-MIT
    │   ├── dom7-MIT
    │   ├── domify-MIT
    │   ├── driver-dom-BSD-3-Clause
    │   ├── driver-miniapp-BSD-3-Clause
    │   ├── driver-universal-BSD-3-Clause
    │   ├── driver-weex-BSD-3-Clause
    │   ├── dva-MIT
    │   ├── dva-core-MIT
    │   ├── electron-to-chromium-ISC
    │   ├── elliptic-MIT
    │   ├── emotion-cache-MIT
    │   ├── emotion-core-MIT
    │   ├── emotion-css-MIT
    │   ├── emotion-hash-MIT
    │   ├── emotion-is-prop-valid-MIT
    │   ├── emotion-memoize-MIT
    │   ├── emotion-serialize-MIT
    │   ├── emotion-sheet-MIT
    │   ├── emotion-stylis-MIT
    │   ├── emotion-unitless-MIT
    │   ├── emotion-utils-MIT
    │   ├── emotion-weak-memoize-MIT
    │   ├── encoding-MIT
    │   ├── error-ex-MIT
    │   ├── escalade-MIT
    │   ├── escape-string-regexp-MIT
    │   ├── evp_bytestokey-MIT
    │   ├── feelers-MIT
    │   ├── feelin-MIT
    │   ├── fill-range-MIT
    │   ├── find-root-MIT
    │   ├── find-up-MIT
    │   ├── flat-BSD-3-Clause
    │   ├── flatten-MIT
    │   ├── focus-trap-MIT
    │   ├── follow-redirects-MIT
    │   ├── form-data-MIT
    │   ├── fs.realpath-ISC
    │   ├── fsevents-MIT
    │   ├── function-bind-MIT
    │   ├── gensync-MIT
    │   ├── get-caller-file-ISC
    │   ├── glob-ISC
    │   ├── glob-parent-ISC
    │   ├── global-MIT
    │   ├── globals-MIT
    │   ├── growl-MIT
    │   ├── h2-MPL-2.0
    │   ├── hamcrest-BSD-3-Clause
    │   ├── hammerjs-MIT
    │   ├── has-flag-MIT
    │   ├── hash-base-MIT
    │   ├── hash.js-MIT
    │   ├── hasown-MIT
    │   ├── he-MIT
    │   ├── history-MIT
    │   ├── hmac-drbg-MIT
    │   ├── hoist-non-react-statics-BSD-3-Clause
    │   ├── iconv-lite-MIT
    │   ├── icu4j-Unicode
    │   ├── import-fresh-MIT
    │   ├── inflight-ISC
    │   ├── inherits-ISC
    │   ├── inherits-browser-ISC
    │   ├── invariant-MIT
    │   ├── is-arrayish-MIT
    │   ├── is-binary-path-MIT
    │   ├── is-core-module-MIT
    │   ├── is-extglob-MIT
    │   ├── is-fullwidth-code-point-MIT
    │   ├── is-glob-MIT
    │   ├── is-number-MIT
    │   ├── is-plain-obj-MIT
    │   ├── is-plain-object-MIT
    │   ├── is-what-MIT
    │   ├── isarray-MIT
    │   ├── isexe-ISC
    │   ├── isobject-MIT
    │   ├── isomorphic-fetch-MIT
    │   ├── janino-BSD-3-Clause
    │   ├── jedis-MIT
    │   ├── jquery-MIT
    │   ├── jridgewell-gen-mapping-MIT
    │   ├── jridgewell-resolve-uri-MIT
    │   ├── jridgewell-set-array-MIT
    │   ├── jridgewell-sourcemap-codec-MIT
    │   ├── jridgewell-trace-mapping-MIT
    │   ├── js-tokens-MIT
    │   ├── jsesc-MIT
    │   ├── json-parse-even-better-errors-MIT
    │   ├── json5-MIT
    │   ├── jul-to-slf4j-MIT
    │   ├── junit4-EPL-1.0
    │   ├── kryo-BSD-3-Clause
    │   ├── lang-feel-MIT
    │   ├── lezer-common-MIT
    │   ├── lezer-feel-MIT
    │   ├── lezer-highlight-MIT
    │   ├── lezer-lr-MIT
    │   ├── lezer-markdown-MIT
    │   ├── lines-and-columns-MIT
    │   ├── loader-utils-MIT
    │   ├── locate-path-MIT
    │   ├── lodash-MIT
    │   ├── lodash-es-MIT
    │   ├── lodash.clonedeep-MIT
    │   ├── log-symbols-MIT
    │   ├── loose-envify-MIT
    │   ├── lru-cache-ISC
    │   ├── luxon-MIT
    │   ├── md5.js-MIT
    │   ├── memoize-one-MIT
    │   ├── merge-anything-MIT
    │   ├── mime-db-MIT
    │   ├── mime-types-MIT
    │   ├── min-dash-MIT
    │   ├── min-document-MIT
    │   ├── min-dom-MIT
    │   ├── minimalistic-assert-ISC
    │   ├── minimalistic-crypto-utils-MIT
    │   ├── minimatch-ISC
    │   ├── minlog-BSD-3-Clause
    │   ├── mocha-MIT
    │   ├── moment-MIT
    │   ├── ms-MIT
    │   ├── mxparser-IUELSL
    │   ├── nanoid-MIT
    │   ├── node-fetch-MIT
    │   ├── node-releases-MIT
    │   ├── normalize-path-MIT
    │   ├── object-assign-MIT
    │   ├── object-refs-MIT
    │   ├── omit.js-MIT
    │   ├── once-ISC
    │   ├── p-limit-MIT
    │   ├── p-locate-MIT
    │   ├── parent-module-MIT
    │   ├── parse-asn1-ISC
    │   ├── parse-json-MIT
    │   ├── path-exists-MIT
    │   ├── path-intersection-MIT
    │   ├── path-is-absolute-MIT
    │   ├── path-parse-MIT
    │   ├── path-to-regexp-MIT
    │   ├── path-type-MIT
    │   ├── pbkdf2-MIT
    │   ├── picocolors-ISC
    │   ├── picomatch-MIT
    │   ├── postcss-value-parse-MIT
    │   ├── postcss-value-parser-MIT
    │   ├── postgresql-BSD-2-Clause
    │   ├── preact-MIT
    │   ├── process-MIT
    │   ├── process-nextick-args-MIT
    │   ├── prop-types-MIT
    │   ├── protobuf-java-BSD-3-Clause
    │   ├── proxy-from-env-MIT
    │   ├── randombytes-MIT
    │   ├── rax-BSD-3-Clause
    │   ├── react-MIT
    │   ├── react-dom-MIT
    │   ├── react-is-MIT
    │   ├── react-lifecycles-compat-MIT
    │   ├── react-loading-skeleton-MIT
    │   ├── react-redux-MIT
    │   ├── react-router-MIT
    │   ├── react-router-dom-MIT
    │   ├── react-router-redux-MIT
    │   ├── react-transition-group-BSD-3-Clause
    │   ├── readable-stream-MIT
    │   ├── readdirp-MIT
    │   ├── redux-MIT
    │   ├── redux-saga-MIT
    │   ├── redux-thunk-MIT
    │   ├── reflectasm-BSD-3-Clause
    │   ├── regenerator-runtime-MIT
    │   ├── require-directory-MIT
    │   ├── resize-observer-polyfill-MIT
    │   ├── resolve-MIT
    │   ├── resolve-from-MIT
    │   ├── resolve-pathname-MIT
    │   ├── ripemd160-MIT
    │   ├── safe-buffer-MIT
    │   ├── safer-buffer-MIT
    │   ├── scheduler-MIT
    │   ├── semver-ISC
    │   ├── serialize-javascript-BSD-3-Clause
    │   ├── sha.js-MIT
    │   ├── shallow-element-equals-MIT
    │   ├── slf4j-api-MIT
    │   ├── source-map-BSD-3-Clause
    │   ├── sprintf-js-BSD-3-Clause
    │   ├── ssr-window-MIT
    │   ├── string-decoder-MIT
    │   ├── string-width-MIT
    │   ├── string_decoder-MIT
    │   ├── strip-ansi-MIT
    │   ├── strip-json-comments-MIT
    │   ├── style-equal-MIT
    │   ├── style-mod-MIT
    │   ├── styled-components-MIT
    │   ├── stylis-MIT
    │   ├── stylis-rule-sheet-MIT
    │   ├── supports-color-MIT
    │   ├── supports-preserve-symlinks-flag-MIT
    │   ├── svelte-MIT
    │   ├── swiper-MIT
    │   ├── symbol-observable-MIT
    │   ├── tabbable-MIT
    │   ├── tiny-invariant-MIT
    │   ├── tiny-svg-MIT
    │   ├── tiny-warning-MIT
    │   ├── to-fast-properties-MIT
    │   ├── to-regex-range-MIT
    │   ├── tr46-MIT
    │   ├── tslib-OBSD
    │   ├── types-history-MIT
    │   ├── types-hoist-non-react-statics-MIT
    │   ├── types-isomorphic-fetch-MIT
    │   ├── types-parse-json-MIT
    │   ├── types-prop-types-MIT
    │   ├── types-react-MIT
    │   ├── types-react-dom-MIT
    │   ├── types-react-router-MIT
    │   ├── types-react-router-dom-MIT
    │   ├── types-react-router-redux-MIT
    │   ├── types-scheduler-MIT
    │   ├── types-use-sync-external-store-MIT
    │   ├── ungap-ISC
    │   ├── uni-BSD-3-Clause
    │   ├── universal-BSD-3-Clause
    │   ├── update-browserslist-db-MIT
    │   ├── use-sync-external-store-MIT
    │   ├── util-deprecate-MIT
    │   ├── value-equal-MIT
    │   ├── w3c-keyname-MIT
    │   ├── warning-BSD-3-Clause
    │   ├── warning-MIT
    │   ├── webidl-conversions-BSD-2-Clause
    │   ├── whatwg-fetch-MIT
    │   ├── whatwg-url-MIT
    │   ├── which-ISC
    │   ├── wide-align-ISC
    │   ├── wrap-ansi-MIT
    │   ├── wrappy-ISC
    │   ├── xstream-BSD-3-Clause
    │   ├── y18n-ISC
    │   ├── yallist-ISC
    │   ├── yaml-ISC
    │   ├── yamljs-MIT
    │   ├── yargs-MIT
    │   ├── yargs-parser-ISC
    │   ├── yargs-unparser-MIT
    │   ├── yocto-queue-MIT
    │   └── zstd-jni-BSD-2-Clause
    ├── script
    │   ├── config-center
    │   │   ├── README.md
    │   │   ├── apollo
    │   │   │   ├── apollo-config-interactive.sh
    │   │   │   └── apollo-config.sh
    │   │   ├── config.txt
    │   │   ├── consul
    │   │   │   ├── consul-config-interactive.sh
    │   │   │   └── consul-config.sh
    │   │   ├── etcd3
    │   │   │   ├── etcd3-config-interactive.sh
    │   │   │   └── etcd3-config.sh
    │   │   ├── nacos
    │   │   │   ├── nacos-config-interactive.py
    │   │   │   ├── nacos-config-interactive.sh
    │   │   │   ├── nacos-config.py
    │   │   │   └── nacos-config.sh
    │   │   └── zk
    │   │       ├── zk-config-interactive.sh
    │   │       └── zk-config.sh
    │   ├── logstash
    │   │   └── config
    │   │       ├── logstash-kafka.conf
    │   │       └── logstash-logback.conf
    │   └── server
    │       ├── db
    │       │   ├── dm.sql
    │       │   ├── kingbase.sql
    │       │   ├── mysql.sql
    │       │   ├── oracle.sql
    │       │   ├── oscar.sql
    │       │   ├── postgresql.sql
    │       │   └── sqlserver.sql
    │       ├── docker-compose
    │       │   └── docker-compose.yaml
    │       ├── helm
    │       │   └── seata-server
    │       │       ├── Chart.yaml
    │       │       ├── templates
    │       │       │   ├── NOTES.txt
    │       │       │   ├── _helpers.tpl
    │       │       │   ├── deployment.yaml
    │       │       │   ├── service.yaml
    │       │       │   └── tests
    │       │       │       └── test-connection.yaml
    │       │       └── values.yaml
    │       └── kubernetes
    │           └── seata-server.yaml
    └── target
        └── seata-server.jar
        
```