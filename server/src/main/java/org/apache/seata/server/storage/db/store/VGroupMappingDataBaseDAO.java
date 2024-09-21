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
package org.apache.seata.server.storage.db.store;

import org.apache.seata.common.exception.ErrorCode;
import org.apache.seata.common.exception.SeataRuntimeException;
import org.apache.seata.common.metadata.namingserver.Instance;
import org.apache.seata.common.util.IOUtil;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.store.MappingDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.apache.seata.common.ConfigurationKeys.VGROUP_TABLE_NAME;
import static org.apache.seata.common.ConfigurationKeys.REGISTRY_NAMINGSERVER_CLUSTER;
import static org.apache.seata.common.NamingServerConstants.DEFAULT_VGROUP_MAPPING;


public class VGroupMappingDataBaseDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(VGroupMappingDataBaseDAO.class);

    protected DataSource vGroupMappingDataSource;

    protected final String vMapping;

    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();

    public VGroupMappingDataBaseDAO(DataSource vGroupMappingDataSource) {
        this.vGroupMappingDataSource = vGroupMappingDataSource;
        this.vMapping = CONFIG.getConfig(VGROUP_TABLE_NAME, DEFAULT_VGROUP_MAPPING);
    }

    public boolean insertMappingDO(MappingDO mappingDO) {
        String sql = "INSERT INTO " + vMapping + " (vgroup,namespace, cluster) VALUES (?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            int index = 1;
            conn = vGroupMappingDataSource.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement(sql);
            ps.setString(index++, mappingDO.getVGroup());
            ps.setString(index++, mappingDO.getNamespace());
            ps.setString(index++, mappingDO.getCluster());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new SeataRuntimeException(ErrorCode.ERR_CONFIG, e);
        } finally {
            IOUtil.close(ps, conn);
        }
    }

    public boolean clearMappingDOByVGroup(String vGroup) {
        String sql = "DELETE FROM " + vMapping + " WHERE vGroup = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = vGroupMappingDataSource.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement(sql);
            ps.setString(1, vGroup);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new SeataRuntimeException(ErrorCode.ERR_CONFIG, e);
        } finally {
            IOUtil.close(ps, conn);
        }
    }

    public boolean deleteMappingDOByVGroup(String vGroup) {
        String sql = "DELETE FROM " + vMapping + " WHERE vGroup = ? and cluster = ?";
        Instance instance = Instance.getInstance();
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = vGroupMappingDataSource.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, vGroup);
            ps.setString(2, instance.getClusterName());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new SeataRuntimeException(ErrorCode.ERROR_SQL, e);
        } finally {
            IOUtil.close(ps, conn);
        }
    }

    public List<MappingDO> queryMappingDO() {
        String sql = "SELECT vgroup,namespace, cluster FROM " + vMapping
                + " WHERE cluster = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<MappingDO> result = new ArrayList<>();

        try {
            conn = vGroupMappingDataSource.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, CONFIG.getConfig(REGISTRY_NAMINGSERVER_CLUSTER));
            rs = ps.executeQuery();

            while (rs.next()) {
                MappingDO mappingDO = new MappingDO();
                mappingDO.setNamespace(rs.getString("namespace"));
                mappingDO.setCluster(rs.getString("cluster"));
                mappingDO.setVGroup(rs.getString("vGroup"));
                result.add(mappingDO);
            }
        } catch (SQLException e) {
            throw new SeataRuntimeException(ErrorCode.ERR_CONFIG, e);
        } finally {
            IOUtil.close(rs, ps, conn);
        }

        return result;
    }


}
