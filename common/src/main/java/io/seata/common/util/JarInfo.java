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
package io.seata.common.util;

import java.net.URL;
import java.util.Objects;
import java.util.jar.Attributes;

/**
 * Jar Info
 *
 * @author wang.liang
 */
public class JarInfo {

    private final URL url;

    private final String name;

    private final String version;

    private final long versionLong;

    /**
     * the attributes of the file 'META-INF/MANIFEST.MF'
     */
    private final Attributes manifestAttributes;

    public JarInfo(URL url, String name, Attributes manifestAttributes, String version) {
        if (url == null) {
            throw new IllegalArgumentException("'url' must not be null");
        }
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("'name' must not be blank");
        }

        this.url = url;
        this.name = name.toLowerCase();
        this.manifestAttributes = manifestAttributes;
        if (StringUtils.isBlank(version)) {
            this.version = VersionUtils.UNKNOWN_VERSION;
            this.versionLong = 0L;
        } else {
            this.version = version;
            this.versionLong = VersionUtils.toLong(version);
        }
    }

    /**
     * compare to version
     *
     * @param otherVersion the other version
     * @return 比较结果：1=higher than otherVersion、0=equals、-1=lower than otherVersion
     */
    public int compareToVersion(String otherVersion) {
        if (Objects.equals(this.version, otherVersion)) {
            return 0;
        } else if (this.version == null) {
            return -1;
        }

        long otherLongVersion = VersionUtils.toLong(otherVersion);
        if (this.versionLong == otherLongVersion) {
            return this.version.compareTo(otherVersion);
        } else if (this.versionLong > otherLongVersion) {
            return 1;
        } else {
            return 0;
        }
    }


    //region Getter

    public URL getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public long getVersionLong() {
        return versionLong;
    }

    public Attributes getAttributes() {
        return manifestAttributes;
    }

    public String getAttribute(Attributes.Name name) {
        return manifestAttributes.getValue(name);
    }

    public String getAttribute(String name) {
        return manifestAttributes.getValue(name);
    }

    //endregion
}
