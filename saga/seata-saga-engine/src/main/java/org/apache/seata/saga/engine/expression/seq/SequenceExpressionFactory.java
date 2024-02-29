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

import org.apache.seata.common.util.StringUtils;
import org.apache.seata.saga.engine.expression.Expression;
import org.apache.seata.saga.engine.expression.ExpressionFactory;
import org.apache.seata.saga.engine.sequence.SeqGenerator;

/**
 * Sequence expression factory
 */
public class SequenceExpressionFactory implements ExpressionFactory {

    private SeqGenerator seqGenerator;

    @Override
    public Expression createExpression(String expressionString) {

        SequenceExpression sequenceExpression = new SequenceExpression();
        sequenceExpression.setSeqGenerator(this.seqGenerator);
        if (StringUtils.hasLength(expressionString)) {
            String[] strings = expressionString.split("\\|");
            if (strings.length >= 2) {
                sequenceExpression.setEntity(strings[0]);
                sequenceExpression.setRule(strings[1]);
            }
        }
        return sequenceExpression;
    }

    public SeqGenerator getSeqGenerator() {
        return seqGenerator;
    }

    public void setSeqGenerator(SeqGenerator seqGenerator) {
        this.seqGenerator = seqGenerator;
    }
}
