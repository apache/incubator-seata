
package com.demo.docker.file;

import lombok.Data;

import java.util.Map;



/**
 * Data Model for docker-compose file
 */
@Data
public final class DockerComposeFile {
    // Attributes will be assembled when reading the yaml file
    private String version;
    private Map<String, Map<String, Object>> services;
    private Map<String, Map<String, Object>> networks;

}
