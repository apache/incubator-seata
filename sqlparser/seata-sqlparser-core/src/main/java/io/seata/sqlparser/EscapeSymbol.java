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
package io.seata.sqlparser;

/**
 * The escape symbol of keyword
 *
 * @author goodboycoder
 */
public class EscapeSymbol {
    private final char leftSymbol;
    private final char rightSymbol;

    public EscapeSymbol(char singleSymbol) {
        this.leftSymbol = singleSymbol;
        this.rightSymbol = singleSymbol;
    }

    public EscapeSymbol(char leftSymbol, char rightSymbol) {
        this.leftSymbol = leftSymbol;
        this.rightSymbol = rightSymbol;
    }

    public char getLeftSymbol() {
        return leftSymbol;
    }

    public char getRightSymbol() {
        return rightSymbol;
    }
}
