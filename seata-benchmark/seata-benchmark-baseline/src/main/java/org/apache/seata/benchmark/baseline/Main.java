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
package org.apache.seata.benchmark.baseline;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class Main {

    /**
     * TODO support api baseline diff integrate into ci, not only dev
     */
    public static void main(String[] args) throws RunnerException, IOException {
        runBenchmark();

        BenchmarkChartGenerator.getInstance().generateChart(getResultFilePath());
    }

    private static void runBenchmark() throws RunnerException {
        String resultFilePath = getResultFilePath();

        Options opt = new OptionsBuilder()
                //.include(JMHExampleBenchmark.class.getSimpleName())
                .include(".*")
                .warmupTime(new TimeValue(1, TimeUnit.SECONDS))
                .warmupIterations(1)
                .measurementIterations(1)
                .measurementTime(new TimeValue(1, TimeUnit.SECONDS))
                .resultFormat(ResultFormatType.JSON)
                //-rff
                .result(resultFilePath)
                .build();

        new Runner(opt).run();
    }

    private static String getResultFilePath() {
        String modulePath = Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("")).getPath();
        return modulePath + "result.json";
    }
}