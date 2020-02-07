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
package io.seata.rm.datasource.undo.parser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.seata.common.Constants;
import io.seata.common.loader.LoadLevel;
import io.seata.rm.datasource.undo.BranchUndoLog;
import io.seata.rm.datasource.undo.UndoLogParser;

import java.io.IOException;
import java.sql.Timestamp;

/**
 * The type Json based undo log parser.
 *
 * @author sharajava
 */
@LoadLevel(name = GsonUndoLogParser.NAME)
public class GsonUndoLogParser implements UndoLogParser {

    public static final String NAME = "gson";

    private static TypeAdapterFactory timestampFactory = new TimestampTypeAdapterFactory();

    private static Gson gson;

    static {
        gson = new GsonBuilder()
                .serializeNulls()
                .registerTypeAdapterFactory(timestampFactory)
                .create();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public byte[] getDefaultContent() {
        return "{}".getBytes(Constants.DEFAULT_CHARSET);
    }

    @Override
    public byte[] encode(BranchUndoLog branchUndoLog) {
        String json = gson.toJson(branchUndoLog);
        return json.getBytes(Constants.DEFAULT_CHARSET);
    }

    @Override
    public BranchUndoLog decode(byte[] bytes) {
        String text = new String(bytes, Constants.DEFAULT_CHARSET);
        return gson.fromJson(text, BranchUndoLog.class);
    }

    private static class TimestampTypeAdapterFactory implements TypeAdapterFactory {

        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (type.getRawType() != Timestamp.class) {
                return null;
            }

            final TypeAdapter<Long> dateTypeAdapter = gson.getAdapter(Long.class);
            return (TypeAdapter<T>) new TypeAdapter<Timestamp>() {
                @Override
                public Timestamp read(JsonReader in) throws IOException {
                    Long date = dateTypeAdapter.read(in);
                    return date != null ? new Timestamp(date) : null;
                }

                @Override
                public void write(JsonWriter out, Timestamp value) throws IOException {
                    dateTypeAdapter.write(out, value.getTime());
                }
            };
        }
    }
}
