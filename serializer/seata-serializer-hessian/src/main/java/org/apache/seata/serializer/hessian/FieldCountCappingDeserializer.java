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
package org.apache.seata.serializer.hessian;

import com.caucho.hessian.io.AbstractHessianInput;
import com.caucho.hessian.io.Deserializer;

import java.io.IOException;

/**
 * Delegates to a Hessian {@link Deserializer} but rejects absurd
 * {@code createFields(len)} values before the delegate allocates proportional
 * to {@code len} (mitigates tiny-payload OOM on malicious Hessian class defs).
 */
final class FieldCountCappingDeserializer implements Deserializer {

    private final Deserializer delegate;
    private final int maxClassFields;

    FieldCountCappingDeserializer(Deserializer delegate, int maxClassFields) {
        this.delegate = delegate;
        this.maxClassFields = maxClassFields;
    }

    @Override
    public Class<?> getType() {
        return delegate.getType();
    }

    @Override
    public boolean isReadResolve() {
        return delegate.isReadResolve();
    }

    @Override
    public Object readObject(AbstractHessianInput in) throws IOException {
        return delegate.readObject(in);
    }

    @Override
    public Object readList(AbstractHessianInput in, int length) throws IOException {
        return delegate.readList(in, length);
    }

    @Override
    public Object readLengthList(AbstractHessianInput in, int length) throws IOException {
        return delegate.readLengthList(in, length);
    }

    @Override
    public Object readMap(AbstractHessianInput in) throws IOException {
        return delegate.readMap(in);
    }

    @Override
    public Object[] createFields(int len) {
        if (len < 0 || len > maxClassFields) {
            throw new IllegalStateException(
                    "Hessian class definition field count " + len + " exceeds maximum " + maxClassFields);
        }
        return delegate.createFields(len);
    }

    @Override
    public Object createField(String name) {
        return delegate.createField(name);
    }

    @Override
    public Object readObject(AbstractHessianInput in, Object[] fields) throws IOException {
        return delegate.readObject(in, fields);
    }

    @Override
    public Object readObject(AbstractHessianInput in, String[] fieldNames) throws IOException {
        return delegate.readObject(in, fieldNames);
    }
}
