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
package io.seata.serializer.seata.protocol.transaction;

import java.io.Serializable;
import java.nio.ByteBuffer;
import io.netty.buffer.ByteBuf;
import io.seata.common.util.StringUtils;
import io.seata.core.protocol.client.ClusterMetaDataResponse;

/**
 * The type Branch report response codec.
 *
 * @author zhangsen
 */
public class ClusterMetaDataResponseCodec extends AbstractTransactionResponseCodec implements Serializable {

    @Override
    public Class<?> getMessageClassType() {
        return ClusterMetaDataResponse.class;
    }


    @Override
    public <T> void encode(T t, ByteBuf out) {
        super.encode(t, out);

        ClusterMetaDataResponse clusterMetaDataResponse = (ClusterMetaDataResponse)t;
        String address = clusterMetaDataResponse.getLeaderAddress();
        if (StringUtils.isNotBlank(address)) {
            byte[] bs = address.getBytes(UTF8);
            out.writeShort((short)bs.length);
            if (bs.length > 0) {
                out.writeBytes(bs);
            }
        } else {
            out.writeShort((short)0);
        }
        String mode = clusterMetaDataResponse.getMode();
        if (StringUtils.isNotBlank(mode)) {
            byte[] bs = mode.getBytes(UTF8);
            out.writeShort((short)bs.length);
            if (bs.length > 0) {
                out.writeBytes(bs);
            }
        } else {
            out.writeShort((short)0);
        }
        String learners = clusterMetaDataResponse.getLearners();
        if (StringUtils.isNotBlank(learners)) {
            byte[] bs = learners.getBytes(UTF8);
            out.writeShort((short)bs.length);
            if (bs.length > 0) {
                out.writeBytes(bs);
            }
        } else {
            out.writeShort((short)0);
        }
        String followers = clusterMetaDataResponse.getFollowers();
        if (StringUtils.isNotBlank(followers)) {
            byte[] bs = followers.getBytes(UTF8);
            out.writeShort((short)bs.length);
            if (bs.length > 0) {
                out.writeBytes(bs);
            }
        } else {
            out.writeShort((short)0);
        }
    }

    @Override
    public <T> void decode(T t, ByteBuffer in) {
        super.decode(t, in);

        ClusterMetaDataResponse clusterMetaDataResponse = (ClusterMetaDataResponse)t;
        short len = in.getShort();
        if (len > 0) {
            byte[] bs = new byte[len];
            in.get(bs);
            clusterMetaDataResponse.setLeaderAddress(new String(bs, UTF8));
        }

        len = in.getShort();
        if (len > 0) {
            byte[] bs = new byte[len];
            in.get(bs);
            clusterMetaDataResponse.setMode(new String(bs, UTF8));
        }
        len = in.getShort();
        if (len > 0) {
            byte[] bs = new byte[len];
            in.get(bs);
            clusterMetaDataResponse.setLearners(new String(bs, UTF8));
        }
        len = in.getShort();
        if (len > 0) {
            byte[] bs = new byte[len];
            in.get(bs);
            clusterMetaDataResponse.setFollowers(new String(bs, UTF8));
        }
    }

}
