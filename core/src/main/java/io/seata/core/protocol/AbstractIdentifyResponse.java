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
package io.seata.core.protocol;

/**
 * The type Abstract identify response.
 *
 * @author sharajava
 */
public abstract class AbstractIdentifyResponse extends AbstractResultMessage {

    private String version = Version.CURRENT;

    private String extraData;

    private boolean identified;

    /**
     * Gets version.
     *
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets version.
     *
     * @param version the version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Gets extra data.
     *
     * @return the extra data
     */
    public String getExtraData() {
        return extraData;
    }

    /**
     * Sets extra data.
     *
     * @param extraData the extra data
     */
    public void setExtraData(String extraData) {
        this.extraData = extraData;
    }

    /**
     * Is identified boolean.
     *
     * @return the boolean
     */
    public boolean isIdentified() {
        return identified;
    }

    /**
     * Sets identified.
     *
     * @param identified the identified
     */
    public void setIdentified(boolean identified) {
        this.identified = identified;
    }


    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("version=");
        result.append(version);
        result.append(",");
        result.append("extraData=");
        result.append(extraData);
        result.append(",");
        result.append("identified=");
        result.append(identified);
        result.append(",");
        result.append("resultCode=");
        result.append(getResultCode());
        result.append(",");
        result.append("msg=");
        result.append(getMsg());

        return result.toString();
    }
}
