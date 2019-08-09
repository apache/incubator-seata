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
package io.seata.rm.datasource.undo.postgresql.keyword;

import java.util.Set;

import io.seata.rm.datasource.undo.KeywordChecker;

/**
 * The type PostgreSQL keyword checker.
 *
 * @author l81893521
 * @date 2019/8/8
 */
public class PostgreSQLKeywordChecker implements KeywordChecker {

    private static volatile KeywordChecker keywordChecker = null;
    private static volatile Set<String> keywordSet = null;

    private PostgreSQLKeywordChecker(){
    }

    /**
     * get instance of postgreSQL keyword checker
     * @return
     */
    public static KeywordChecker getInstance(){
        if(keywordChecker == null){
            synchronized (PostgreSQLKeywordChecker.class){
                if(keywordChecker == null) {
                    keywordChecker =  new PostgreSQLKeywordChecker();
                }
            }
        }
        return keywordChecker;
    }

    /**
     * postgreSQL keyword
     */
    private enum PostgreSQLKeyword {

    }

    @Override
    public boolean check(String fieldOrTableName) {
        return false;
    }

    @Override
    public String checkAndReplace(String fieldOrTableName) {
        return null;
    }
}
