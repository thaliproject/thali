/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

THIS CODE IS PROVIDED ON AN *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED,
INCLUDING WITHOUT LIMITATION ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A PARTICULAR PURPOSE,
MERCHANTABLITY OR NON-INFRINGEMENT.

See the Apache 2 License for the specific language governing permissions and limitations under the License.
*/

package com.msopentech.thali.utilities.universal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ektorp.ReplicationStatus;
import org.ektorp.http.HttpClient;
import org.ektorp.http.HttpResponse;
import org.ektorp.http.RestTemplate;
import org.ektorp.http.StdResponseHandler;
import org.ektorp.impl.ObjectMapperFactory;
import org.ektorp.impl.StdCouchDbInstance;
import org.ektorp.impl.StdObjectMapperFactory;
import org.ektorp.util.Exceptions;

import java.io.IOException;

public class ThaliCouchDbInstance extends StdCouchDbInstance {
    protected final RestTemplate restTemplate;
    protected final ObjectMapper objectMapper;

    public ThaliCouchDbInstance(HttpClient client) {
        super(client);
        restTemplate = new RestTemplate(client);
        objectMapper = (new StdObjectMapperFactory()).createObjectMapper();
    }

    public ThaliCouchDbInstance(HttpClient client, ObjectMapperFactory of) {
        super(client, of);
        restTemplate = new RestTemplate(client);
        objectMapper = of.createObjectMapper();
    }

    public ReplicationStatus replicate(ThaliReplicationCommand cmd) {
        // Note the following code is taken from StdCouchDbInstance.java/replicate from Ektorp
        try {
            return restTemplate.post("/_replicate", objectMapper.writeValueAsString(cmd), new StdResponseHandler<ReplicationStatus>() {
                @Override
                public ReplicationStatus success(HttpResponse hr)
                        throws Exception {
                    return objectMapper.readValue(hr.getContent(), ReplicationStatus.class);
                }
            });
        } catch (IOException e) {
            throw Exceptions.propagate(e);
        }
    }
}
