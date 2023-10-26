/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.seata.saga.statelang.builder.prop;

/***
 * State property builder.
 *
 * @param <P> property builder type
 * @author ptyin
 */
public interface BasicPropertyBuilder<P extends BasicPropertyBuilder<P>> {
    /**
     * Configure name.
     *
     * @param name name of state
     * @return builder for chaining
     */
    P withName(String name);

    /**
     * Configure comment.
     *
     * @param comment comment of state
     * @return builder for chaining
     */
    P withComment(String comment);

    /**
     * Configure next state.
     *
     * @param next next state
     * @return builder for chaining
     */
    P withNext(String next);
}
