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
package org.apache.seata.benchmark.profiler.base;

public class ProfilerTemplate {

    public static void executeProfile(Runnable runnable) throws Exception {
        executeProfile(ProfilerType.async_profiler, EventType.cpu, runnable);
    }

    public static void executeProfile(Runnable runnable, int warmUpIterations, int profileIterations) throws Exception {
        executeProfile(ProfilerType.async_profiler, EventType.cpu, runnable, warmUpIterations, profileIterations);
    }


    public static void executeProfile(ProfilerType profilerType, EventType eventType, Runnable runnable, int warmUpIterations, int profileIterations) throws Exception {
        Profiler profiler = ProfilerFactory.getProfiler(profilerType);
        profiler.start();
        profiler.profile(runnable, eventType, warmUpIterations, profileIterations);
    }

    public static void executeProfile(ProfilerType profilerType, EventType eventType, Runnable runnable) throws Exception {
        Profiler profiler = ProfilerFactory.getProfiler(profilerType);
        profiler.start();
        profiler.profile(runnable, eventType);
    }

}
