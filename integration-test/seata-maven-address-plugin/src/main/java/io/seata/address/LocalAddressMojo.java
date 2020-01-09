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
package io.seata.address;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "local-address")
public class LocalAddressMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}")
    private MavenProject project;

    @Parameter(name = "localAddress", defaultValue = "seata.local.address")
    private String localAddress;


    @Override
    public void execute() {
        String address = SeataLocalIpDetector.getAddress();
        getLog().info("set seata local address " + address + " to property '" + localAddress + "'");
        project.getProperties().setProperty(localAddress, SeataLocalIpDetector.getAddress());
    }

    public String getLocalAddress() {
        return localAddress;
    }

    public void setLocalAddress(String localAddress) {
        this.localAddress = localAddress;
    }
}
