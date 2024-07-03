/*
 * Copyright 2014-2023 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jkoolcloud.tnt4j.streams.inputs;

import com.jkoolcloud.tnt4j.sink.EventSink;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;

/**
 * @author slb
 * @version 1.0
 * @created 2023-12-12 11:00
 */
public class GRPCStream extends AbstractBufferedStream<>{

    @Override
    protected long getActivityItemByteSize(Object activityItem) {
        return 0;
    }

    @Override
    protected boolean isInputEnded() {
        return false;
    }

    @Override
    protected EventSink logger() {
        return null;
    }

    private class GRPCDataReceiver extends InputProcessor {
        private Server server;

        private GRPCDataReceiver() {
            super("GRPCStream.GRPCDataReceiver"); // NON-NLS
        }

        @Override
        protected void initialize(Object... params) throws Exception {
//            server = ServerBuilder.forPort ((int) params[0])
//                .addService ()
//                .build ();

            server = Grpc.newServerBuilderForPort((int) params[0], InsecureServerCredentials.create ())
                .addService ()
                .build ();

            server.start();
        }

        @Override
        void closeInternals() throws Exception {

        }

    }
}
