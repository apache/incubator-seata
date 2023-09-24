/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aot.hint;

/**
 * MemberCategory's fake
 *
 * @author Andy Clement
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @since 6.0
 */
public enum MemberCategory {

    PUBLIC_FIELDS,

    DECLARED_FIELDS,

    INTROSPECT_PUBLIC_CONSTRUCTORS,

    INTROSPECT_DECLARED_CONSTRUCTORS,

    INVOKE_PUBLIC_CONSTRUCTORS,

    INVOKE_DECLARED_CONSTRUCTORS,

    INTROSPECT_PUBLIC_METHODS,

    INTROSPECT_DECLARED_METHODS,

    INVOKE_PUBLIC_METHODS,

    INVOKE_DECLARED_METHODS,

    PUBLIC_CLASSES,

    DECLARED_CLASSES

}
