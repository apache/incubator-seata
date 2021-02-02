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
package io.seata.rm.datasource;

import io.seata.core.model.BranchStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AsyncWorkerTest {

    private final AsyncWorker worker = new AsyncWorker(null);

    private final Random random = new Random();

    @Test
    void branchCommit() {
        BranchStatus status = worker.branchCommit("test", 0, null);
        assertEquals(BranchStatus.PhaseTwo_Committed, status, "should return PhaseTwo_Committed");
    }

    @Test
    void doBranchCommitSafely() {
        assertDoesNotThrow(worker::doBranchCommitSafely, "this method should never throw anything");
    }

    @Test
    void groupedByResourceId() {
        List<AsyncWorker.Phase2Context> contexts = getRandomContexts();
        Map<String, List<AsyncWorker.Phase2Context>> groupedContexts = worker.groupedByResourceId(contexts);
        groupedContexts.forEach((resourceId, group) -> group.forEach(context -> {
            String message = "each context in the group should has the same resourceId";
            assertEquals(resourceId, context.resourceId, message);
        }));
    }

    private List<AsyncWorker.Phase2Context> getRandomContexts() {
        return random.ints().limit(16)
                .mapToObj(String::valueOf)
                .flatMap(this::generateContextStream)
                .collect(Collectors.toList());
    }

    private Stream<AsyncWorker.Phase2Context> generateContextStream(String resourceId) {
        int size = random.nextInt(10);
        return IntStream.range(0, size).mapToObj(i -> buildContext(resourceId));
    }

    private AsyncWorker.Phase2Context buildContext(String resourceId) {
        return new AsyncWorker.Phase2Context("test", 0, resourceId);
    }
}