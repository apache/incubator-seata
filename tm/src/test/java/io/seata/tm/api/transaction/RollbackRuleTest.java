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
package io.seata.tm.api.transaction;

import io.seata.common.exception.ShouldNeverHappenException;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
/**
 * @author guoyao
 * @date 2019/4/18
 */
public class RollbackRuleTest {

    @Test
    public void foundImmediatelyWithString() {
        RollbackRule rr = new RollbackRule(Exception.class.getName());
        assertThat(rr.getDepth(new Exception())).isEqualTo(0);
    }

    @Test
    public void RollbackRule() {
        RollbackRule rr = new RollbackRule(Exception.class);
        assertThat(rr.getDepth(new Exception())).isEqualTo(0);
    }

    @Test
    public void notFound() {
        RollbackRule rr = new RollbackRule(java.io.IOException.class.getName());
        assertThat(rr.getDepth(new MyRuntimeException(""))).isEqualTo(-1);
    }

    @Test
    public void ancestry() {
        RollbackRule rr = new RollbackRule(java.lang.Exception.class.getName());
        // Exception -> Runtime -> MyRuntimeException
        assertThat(rr.getDepth(new MyRuntimeException(""))).isEqualTo(2);
    }

    @Test
    public void alwaysTrueForThrowable() {
        RollbackRule rr = new RollbackRule(java.lang.Throwable.class.getName());
        assertThat(rr.getDepth(new MyRuntimeException("")) > 0).isTrue();
        assertThat(rr.getDepth(new IOException()) > 0).isTrue();
        assertThat(rr.getDepth(new ShouldNeverHappenException(null,null)) > 0).isTrue();
        assertThat(rr.getDepth(new RuntimeException()) > 0).isTrue();
    }

    @Test(expected = IllegalArgumentException.class)
    public void ctorArgMustBeAThrowableClassWithNonThrowableType() {
        new RollbackRule(String.class);
    }

    @Test(expected = NullPointerException.class)
    public void ctorArgMustBeAThrowableClassWithNullThrowableType() {
        new RollbackRule((Class<?>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ctorArgExceptionStringNameVersionWithNull() {
        new RollbackRule((String) null);
    }

}
