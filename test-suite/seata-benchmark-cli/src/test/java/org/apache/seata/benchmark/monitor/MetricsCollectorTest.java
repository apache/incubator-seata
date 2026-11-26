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
package org.apache.seata.benchmark.monitor;

import org.apache.seata.benchmark.config.BenchmarkConfig;
import org.apache.seata.benchmark.constant.BenchmarkConstants;
import org.apache.seata.benchmark.model.BenchmarkMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricsCollectorTest {

    @Test
    @ResourceLock(Resources.LOCALE)
    void exportsTwoColumnsWithDecimalValuesUnderGermanLocale(@TempDir Path directory) throws Exception {
        BenchmarkMetrics metrics = new BenchmarkMetrics();
        metrics.recordTransaction(BenchmarkConstants.STATUS_COMMITTED, 10);
        metrics.recordTransaction(BenchmarkConstants.STATUS_FAILED, 20);
        Path report = directory.resolve("benchmark.csv");

        Locale previous = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.GERMANY);
            new MetricsCollector(new BenchmarkConfig(), metrics).exportToCsv(report.toString());
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, previous);
        }

        List<String> rows = Files.readAllLines(report);
        assertEquals("Metric,Value", rows.get(0));
        for (String row : rows) {
            assertEquals(2, row.split(",", -1).length, row);
        }
        assertTrue(rows.contains("Success Rate (%),50.00"));
        assertTrue(rows.contains("Committed Rate (%),50.00"));
    }
}
