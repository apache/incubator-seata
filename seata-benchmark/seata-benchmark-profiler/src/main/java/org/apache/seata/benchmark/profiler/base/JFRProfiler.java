package org.apache.seata.benchmark.profiler.base;

import org.apache.seata.common.exception.NotSupportYetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.Objects;

public class JFRProfiler implements Profiler {

    public static final Logger LOGGER = LoggerFactory.getLogger(AsyncProfiler.class);

    private String pid;

    @Override
    public void start() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        this.pid = name.split("@")[0];
    }

    @Override
    public void profile(Runnable runnable, EventType eventType) throws Exception {
        String JFRUnLockResponse = runCommand("jcmd", pid, "VM.unlock_commercial_features");
        LOGGER.info("JFR unLock: {}", JFRUnLockResponse);

        String name = "seata-profile";

        //TODO append event
        String[] JFRStartCommand = {"jcmd", pid, "JFR.start", "name=" + name};
        String JFRStartResponse = runCommand(JFRStartCommand);
        LOGGER.info("JFR start: {}", JFRStartResponse);

        runnable.run();

        String[] JFRDumpCommand = {"jcmd", pid, "JFR.dump", "name=" + name, "filename=" + getDefaultJFRFilePath()};
        String JFRDumpResponse = runCommand(JFRDumpCommand);
        LOGGER.info("JFR dump: {}", JFRDumpResponse);

        String[] JFRStopCommand = {"jcmd", pid, "JFR.stop", "name=" + name};
        String JFRStopResponse = runCommand(JFRStopCommand);

        LOGGER.info("JFR stop: {}", JFRStopResponse);
    }

    private String getDefaultJFRFilePath() {
        String modulePath = Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("")).getPath();
        return modulePath + "jfr/" + "recording.jfr";
    }

    @Override
    public void profile(Runnable runnable, EventType eventType, int warmUpIterations, int profileIterations) throws Exception {
        throw new NotSupportYetException();
    }

    @Override
    public void destroy() {

    }

    private String runCommand(String... command) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = processBuilder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        return stringBuilder.toString();
    }
}
