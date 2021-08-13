package com.demo.docker.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;

import java.nio.file.Path;

import lombok.experimental.Delegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ContainerLogger implements Logger {
    @Delegate
    private final Logger delegate;

    public ContainerLogger(final Path logDirectory, final String container) {

        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        final PatternLayoutEncoder encoder = new PatternLayoutEncoder();

        encoder.setPattern("%d{HH:mm:ss.SSS} %-5level %logger{36}.%M - %msg%n");
        encoder.setContext(context);
        encoder.start();

        final FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
        fileAppender.setFile(logDirectory.resolve(container).toAbsolutePath().toString());
        fileAppender.setEncoder(encoder);
        fileAppender.setContext(context);
        fileAppender.start();

        final ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(container);
        logger.addAppender(fileAppender);
        logger.setLevel(Level.DEBUG);
        logger.setAdditive(false);

        this.delegate = logger;
    }
}
