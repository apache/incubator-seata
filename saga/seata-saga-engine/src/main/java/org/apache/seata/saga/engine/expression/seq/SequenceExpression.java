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
package org.apache.seata.saga.engine.expression.seq;

import org.apache.seata.saga.engine.expression.Expression;
import org.apache.seata.saga.engine.sequence.SeqGenerator;

/**
 * Generate sequence expression
 *
 */
public class SequenceExpression implements Expression {

    private SeqGenerator seqGenerator;
    private String entity;
    private String rule;

    @Override
    public Object getValue(Object elContext) {
        return seqGenerator.generate(entity, rule, null);
    }

    @Override
    public void setValue(Object value, Object elContext) {

    }

    @Override
    public String getExpressionString() {
        return this.entity + "|" + this.rule;
    }

    public SeqGenerator getSeqGenerator() {
        return seqGenerator;
    }

    public void setSeqGenerator(SeqGenerator seqGenerator) {
        this.seqGenerator = seqGenerator;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }
}
