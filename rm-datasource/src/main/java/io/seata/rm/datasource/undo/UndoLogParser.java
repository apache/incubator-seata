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
package io.seata.rm.datasource.undo;

/**
 * The interface Undo log parser.
 *
 * @author sharajava
 * @author Geng Zhang
 */
public interface UndoLogParser {

    /**
     * Get the name of parser;
     * 
     * @return the name of parser
     */
    String getName();

    /**
     * Get default context of this parser
     * 
     * @return the default content if undo log is empty
     */
    byte[] getDefaultContent();

    /**
     * Encode branch undo log to byte array.
     *
     * @param branchUndoLog the branch undo log
     * @return the byte array
     */
    byte[] encode(BranchUndoLog branchUndoLog);

    /**
     * Decode byte array to branch undo log.
     *
     * @param bytes the byte array
     * @return the branch undo log
     */
    BranchUndoLog decode(byte[] bytes);
}
