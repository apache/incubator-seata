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
package io.seata.server.storage.tsdb;

import io.seata.server.storage.tsdb.api.Event;
import io.seata.server.storage.tsdb.api.EventTopic;

import java.util.ArrayList;

public class Handler {

    public void handle(ArrayList<Event> events) {
        Object o = parserEvents(events);
        insertToTsdb(o);
    }

    private Object parserEvents(ArrayList<Event> events) {
        if (events.isEmpty()) {
            return null;
        }
        EventTopic eventTopic = events.get(0).topic;
        switch (eventTopic) {
            case GLOBAL_SESSION:
                break;
            case BRANCH_SESSION:
                break;
            case UNDO:
                break;
        }
        //TODO parser events
        return new Object();
    }

    //TODO batch insert tsdb
    private void insertToTsdb(Object o) {
    }
}
