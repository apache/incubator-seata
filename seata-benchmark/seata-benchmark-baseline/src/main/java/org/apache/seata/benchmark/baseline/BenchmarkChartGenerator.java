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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BenchmarkChartGenerator {

    public static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkChartGenerator.class);

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


    /**
     * Generate chart by jmh json result
     *
     * @param jmhJsonFilePath jmhJsonFilePath
     * @throws IOException IOException
     */
    public void generateChart(String jmhJsonFilePath) throws IOException {
        File file = new File(jmhJsonFilePath);
        if (!file.exists()) {
            throw new RuntimeException("jmh file [" + jmhJsonFilePath + "] not exist!");
        }

        String jsonStr = readFileContent(file);

        List<BenchMarkModel> benchMarkModels = objectMapper.readValue(jsonStr, new TypeReference<List<BenchMarkModel>>() {
        });

        Map<String, DefaultCategoryDataset> datasets = new HashMap<>();
        for (BenchMarkModel benchMarkModel : benchMarkModels) {
            DefaultCategoryDataset dataset = datasets.computeIfAbsent(benchMarkModel.getBenchmarkClassName(), k -> new DefaultCategoryDataset());

            double score = benchMarkModel.getPrimaryMetric().getScore();
            dataset.addValue(score, benchMarkModel.getMode(), benchMarkModel.getBenchmarkMethodKey());
        }


        String modulePath = Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("")).getPath();
        for (Map.Entry<String, DefaultCategoryDataset> datasetEntry : datasets.entrySet()) {
            String className = datasetEntry.getKey();

            JFreeChart barChart = ChartFactory.createBarChart(
                    //chart Name
                    className,
                    //X Name
                    "Method",
                    //Y Name
                    "Score",
                    //dataset
                    datasetEntry.getValue()
            );

            //store to chart
            File chartFile = new File(modulePath + "chart/" + className + ".png");
            ChartUtils.saveChartAsPNG(chartFile, barChart, 640, 480);

            LOGGER.info("Generated chart: " + chartFile.getAbsolutePath());
        }
    }

    public static String readFileContent(File file) throws IOException {
        StringBuilder contentBuilder = new StringBuilder();
        // 使用当前线程的类加载器获取资源作为流
        try (InputStream inputStream = Files.newInputStream(file.toPath());
             Reader reader = new InputStreamReader(inputStream);
             BufferedReader bufferedReader = new BufferedReader(reader)) {

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                contentBuilder.append(line).append(System.lineSeparator());
            }
        }
        return contentBuilder.toString();
    }


    public static BenchmarkChartGenerator getInstance() {
        return BenchmarkChartGeneratorHolder.INSTANCE;
    }

    private static class BenchmarkChartGeneratorHolder {
        private static final BenchmarkChartGenerator INSTANCE = new BenchmarkChartGenerator();
    }

}
