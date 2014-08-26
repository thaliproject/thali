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

/*
The contents of this file are a mildly altered version of ReplicationCommand.java from Ektorp
 */

package com.msopentech.thali.utilities.universal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.ektorp.util.Assert;

import java.util.Collection;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ThaliReplicationCommand  {
    @JsonInclude(JsonInclude.Include.ALWAYS)
    protected class auth {
        @JsonProperty
        public final Object BogusThali = null;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected class sourceOrTargetWithAuth {
        @JsonProperty
        public final String url;
        @JsonProperty
        public final auth auth = new auth();

        public sourceOrTargetWithAuth(String url) {
            assert url != null;
            this.url = url;
        }
    }

    private static final long serialVersionUID = 6919908757724780784L;

    @JsonProperty
    public final Object source;

    @JsonProperty
    public final Object target;

    @JsonProperty
    public final String proxy;

    @JsonProperty
    public final String filter;

    @JsonProperty("doc_ids")
    public final Collection<String> docIds;

    @JsonProperty
    public final Boolean continuous;

    @JsonProperty
    public final Boolean cancel;

    @JsonProperty("managed_replication")
    public final Boolean managedReplication;

    @JsonProperty("query_params")
    public final Object queryParams;

    @JsonProperty("create_target")
    public final Boolean createTarget;

    @JsonProperty("since_seq")
    public final Object sinceSeq;

    private ThaliReplicationCommand(Builder b) {
        source = generateSourceOrTarget(b.source);
        target = generateSourceOrTarget(b.target);
        proxy = b.proxy;
        filter = b.filter;
        docIds = b.docIds;
        continuous = b.continuous ? Boolean.TRUE : null;
        managedReplication = b.managedReplication ? Boolean.TRUE : null;
        cancel = b.cancel ? Boolean.TRUE : null;
        createTarget = b.createTarget ? Boolean.TRUE : null;
        sinceSeq = b.sinceSeqAsLong != null ? b.sinceSeqAsLong : b.sinceSeqAsString;
        queryParams = b.queryParams;
    }

    protected Object generateSourceOrTarget(String url) {
        try {
            new HttpKeyURL(url);
            return new sourceOrTargetWithAuth(url);
        } catch (IllegalArgumentException e) {
            return url;
        }
    }

    public static class Builder {

        private String source;
        private String target;
        private String proxy;
        private String filter;
        private Collection<String> docIds;
        private boolean continuous;
        private boolean cancel;
        private boolean createTarget;
        private String sinceSeqAsString;
        private Long sinceSeqAsLong;
        private Object queryParams;
        private boolean managedReplication;
        /**
         * Source and target can both point at local databases, remote databases and any combination of these.
         *
         * If your local CouchDB instance is secured by an admin account, you need to use the full URL format
         * @param s
         * @return
         */
        public Builder source(String s) {
            source = s;
            return this;
        }

        /**
         * Source and target can both point at local databases, remote databases and any combination of these
         *
         * If your local CouchDB instance is secured by an admin account, you need to use the full URL format.
         * @param s
         * @return
         */
        public Builder target(String s) {
            target = s;
            return this;
        }
        /**
         * Pass a "proxy" argument in the replication data to have replication go through an HTTP proxy
         * @param s
         * @return
         */
        public Builder proxy(String s) {
            proxy = s;
            return this;
        }
        /**
         * Specify a filter function.
         * @param s
         * @return
         */
        public Builder filter(String s) {
            filter = s;
            return this;
        }
        /**
         * Restricts replication to the specified document ids.
         * @param docIds
         * @return
         */
        public Builder docIds(Collection<String> docIds) {
            this.docIds = docIds;
            return this;
        }
        /**
         * true makes replication continuous
         * @param b
         * @return
         */
        public Builder continuous(boolean b) {
            continuous = b;
            return this;
        }
        /**
         * true makes this a Thali managed replication
         *
         */
        public Builder managedReplication(boolean b) {
            managedReplication = b;
            return this;
        }
        /**
         * true cancels a continuous replication task
         * @param b
         * @return
         */
        public Builder cancel(boolean b) {
            cancel = b;
            return this;
        }
        /**
         * Pass parameters to the filter function if specified.
         * @param o
         * @return
         */
        public Builder queryParams(Object o) {
            queryParams = o;
            return this;
        }
        /**
         * To create the target database (remote or local) prior to replication.
         * The names of the source and target databases do not have to be the same.
         * @param b
         * @return
         */
        public Builder createTarget(boolean b) {
            createTarget = b;
            return this;
        }
        /**
         * The sequence from which the replication should start
         * See http://docs.couchdb.org/en/latest/json-structure.html#replication-settings for details
         *
         * CouchDB expects a Long value for the sequence
         * Cloudant expects a String value for the sequence
         *
         * @param sinceSeq as String
         * @return
         */
        public Builder sinceSeq(String sinceSeq) {
            try {
                this.sinceSeqAsLong =  Long.parseLong(sinceSeq);
            } catch (NumberFormatException e) {
                this.sinceSeqAsString = sinceSeq;
            }
            return this;
        }

        public ThaliReplicationCommand build() {
            Assert.hasText(source, "source may not be null or empty");
            Assert.hasText(target, "target may not be null or empty");
            return new ThaliReplicationCommand(this);
        }
    }
}
