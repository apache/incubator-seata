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

import java.util.Map;

public class BenchMarkModel {

    private String benchmark;

    private String mode;

    private Integer warmupIterations;

    private String warmupTime;

    private Integer measurementIterations;

    private String measurementTime;

    private PrimaryMetric primaryMetric;

    private Map<String, String> params;


    public static class PrimaryMetric {

        /**
         * score
         */
        private Double score;

        /**
         * sample time
         */
        private Map<String, Double> scorePercentiles;

        private String scoreUnit;

        public double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }

        public Map<String, Double> getScorePercentiles() {
            return scorePercentiles;
        }

        public void setScorePercentiles(Map<String, Double> scorePercentiles) {
            this.scorePercentiles = scorePercentiles;
        }

        public String getScoreUnit() {
            return scoreUnit;
        }

        public void setScoreUnit(String scoreUnit) {
            this.scoreUnit = scoreUnit;
        }
    }

    public String getBenchmark() {
        return benchmark;
    }

    public void setBenchmark(String benchmark) {
        this.benchmark = benchmark;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Integer getWarmupIterations() {
        return warmupIterations;
    }

    public void setWarmupIterations(Integer warmupIterations) {
        this.warmupIterations = warmupIterations;
    }

    public String getWarmupTime() {
        return warmupTime;
    }

    public void setWarmupTime(String warmupTime) {
        this.warmupTime = warmupTime;
    }

    public Integer getMeasurementIterations() {
        return measurementIterations;
    }

    public void setMeasurementIterations(Integer measurementIterations) {
        this.measurementIterations = measurementIterations;
    }

    public String getMeasurementTime() {
        return measurementTime;
    }

    public void setMeasurementTime(String measurementTime) {
        this.measurementTime = measurementTime;
    }

    public PrimaryMetric getPrimaryMetric() {
        return primaryMetric;
    }

    public void setPrimaryMetric(PrimaryMetric primaryMetric) {
        this.primaryMetric = primaryMetric;
    }


    public String getBenchmarkClassName() {
        String fullClassName = this.benchmark.substring(0, benchmark.lastIndexOf('.'));

        return fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
    }

    public String getBenchmarkMethodKey() {
        String methodName = benchmark.substring(benchmark.lastIndexOf('.') + 1);
        if (params == null || params.isEmpty()) {
            return methodName;
        }

        //support benchmark param
        StringBuilder stringBuilder = new StringBuilder(methodName);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            stringBuilder.append("-");
            stringBuilder.append(entry.getValue());
        }
        return stringBuilder.toString();
    }

    public Map<String, String> getParams() {
        return params;
    }
}
