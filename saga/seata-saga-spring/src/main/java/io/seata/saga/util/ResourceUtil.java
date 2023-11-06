package io.seata.saga.util;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * State lang resource util.
 *
 * @author wang.liang
 */
public class ResourceUtil {

    private static final ResourcePatternResolver RESOURCE_RESOLVER = new PathMatchingResourcePatternResolver();

    public static Resource[] getResources(String location) {
        try {
            return RESOURCE_RESOLVER.getResources(location);
        } catch (IOException e) {
            return new Resource[0];
        }
    }

    public static Resource[] getResources(String[] locationArr) {
        return Stream
            .of(Optional.ofNullable(locationArr).orElse(new String[0]))
            .flatMap(location -> Stream.of(getResources(location)))
            .toArray(Resource[]::new);
    }
}