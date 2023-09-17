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
package io.seata.server.storage.db.store;

import io.seata.common.util.IOUtil;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.store.MappingDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class VGroupMappingDataBaseDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(VGroupMappingDataBaseDAO.class);

    protected DataSource vGroupMappingDataSource = null;

    protected String vMapping;

    private static final String DEFAULT_VGROUP_MAPPING = "mapping_tbl";

    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();

    public VGroupMappingDataBaseDAO(DataSource vGroupMappingDataSource) {
        this.vGroupMappingDataSource = vGroupMappingDataSource;
        vMapping = CONFIG.getConfig("store.db.mapping-table", DEFAULT_VGROUP_MAPPING);
    }

    public boolean insertMappingDO(MappingDO mappingDO) {
        deleteMappingDOByVGroup(mappingDO.getVGroup());
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
            e.printStackTrace();
        } finally {
            IOUtil.close(ps, conn);
        }
        return false;
    }

    public boolean deleteMappingDOByVGroup(String vGroup) {
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
            e.printStackTrace();
        } finally {
            IOUtil.close(ps, conn);
        }
        return false;
    }

    public List<MappingDO> queryMappingDO() {
        String sql = "SELECT vgroup,namespace, cluster FROM " + vMapping;
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<MappingDO> result = new ArrayList<>();

        try {
            conn = vGroupMappingDataSource.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                MappingDO mappingDO = new MappingDO();
                mappingDO.setNamespace(rs.getString("namespace"));
                mappingDO.setCluster(rs.getString("cluster"));
                mappingDO.setVGroup(rs.getString("vGroup"));
                result.add(mappingDO);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            IOUtil.close(rs, ps, conn);
        }

        return result;
    }


}
