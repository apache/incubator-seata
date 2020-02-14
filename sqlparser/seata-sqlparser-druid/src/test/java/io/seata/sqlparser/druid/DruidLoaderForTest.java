package io.seata.sqlparser.druid;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

class DruidLoaderForTest implements DruidLoader {
    @Override
    public URL getEmbeddedDruidLocation() {
        try {
            return URI.create("file://druid-test.jar").toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
