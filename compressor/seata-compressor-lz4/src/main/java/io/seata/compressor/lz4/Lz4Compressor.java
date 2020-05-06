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
package io.seata.compressor.lz4;

import io.seata.common.loader.LoadLevel;
import io.seata.core.compressor.Compressor;

/**
 * the Lz4 Compressor
 *
 * @author diguage
 */
@LoadLevel(name = "LZ4")
public class Lz4Compressor implements Compressor {
    @Override
    public byte[] compress(byte[] bytes) {
        return Lz4Util.compress(bytes);
    }

    @Override
    public byte[] decompress(byte[] bytes) {
        return Lz4Util.decompress(bytes);
    }
}
