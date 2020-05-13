/*
 * (C) Copyright 2015-2019 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 *     Damon Brown
 */
package org.nuxeo.ecm.multi.tenant;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.blob.DefaultBlobDispatcher;
import org.nuxeo.ecm.core.model.Document;

/**
 * Multi-Tenant blob dispatcher, that uses the repository name as the blob provider.
 * <p>
 * Alternatively, it can be configured through properties to dispatch to a blob provider based on document properties
 * instead of the repository name.
 * <p>
 * The property name is a list of comma-separated clauses, with each clause consisting of a property, an operator and a
 * value. The property can be a {@link Document} xpath, {@code ecm:repositoryName}, or, to match the current blob being
 * dispatched, {@code blob:name}, {@code blob:mime-type}, {@code blob:encoding}, {@code blob:digest},
 * {@code blob:length} or {@code blob:xpath}. Comma-separated clauses are ANDed together. The special name
 * {@code default} defines the default provider, and must be present.
 * <p>
 * Binaries may be stored by tenant with use of the {@code ecm:tenant} property. Tenant name is case sensitive.
 * <p>
 * Available operators between property and value are =, !=, &lt, > and ~. The operators &lt; and > work with integer
 * values. The operator ~ does glob matching using {@code ?} to match a single arbitrary character, and {@code *} to
 * match any number of characters (including none).
 * <p>
 * For example, to dispatch to the "first" provider if dc:format is "video", to the "second" provider if the blob's MIME
 * type is "video/mp4", to the "third" provider if the blob is stored as a secondary attached file, to the "fourth"
 * provider if the lifecycle state is "approved" and the document is in the default repository, and otherwise to the
 * "other" provider:
 *
 * <pre>
 * &lt;property name="dc:format=video">first&lt;/property>
 * &lt;property name="blob:mime-type=video/mp4">second&lt;/property>
 * &lt;property name="blob:xpath~files/*&#47;file">third&lt;/property>
 * &lt;property name="ecm:repositoryName=default,ecm:lifeCycleState=approved">fourth&lt;/property>
 * &lt;property name="ecm:tenant=foobar">fifth&lt;/property>
 * &lt;property name="default">other&lt;/property>
 * </pre>
 *
 * @since 7.3
 */
public class MultiTenantBlobDispatcher extends DefaultBlobDispatcher {

    private static final Log log = LogFactory.getLog(MultiTenantBlobDispatcher.class);

    protected static final String TENANT_NAME = "ecm:tenant";

    protected static final String TENANT_FACET = "TenantConfig";

    protected static final String TENANT_XPATH = "tenantconfig:tenantId";

    @Override
    protected Object getValue(Document doc, Blob blob, String blobXPath, Clause clause) {
        String xpath = clause.xpath;
        if (xpath.equals(TENANT_NAME)) {
            return findTenant(doc, clause);
        }
        return super.getValue(doc, blob, blobXPath, clause);
    }

    protected Object getTenant(Document doc) {
        if (doc != null && doc.hasFacet(TENANT_FACET)) {
            return doc.getValue(TENANT_XPATH);
        }
        return null;
    }

    protected Object findTenant(Document doc, Clause clause) {
        Object tenant = getTenant(doc);
        while (tenant == null && doc != null) {
            doc = doc.getParent();
            tenant = getTenant(doc);
        }
        if (tenant == null && doc == null) {
            log.warn("Tenant configuration not found in parent path for dispatch rule: " + clause.xpath + clause.op
                    + clause.value);
            log.warn("Is multi-tenant properly configured?");
        }
        return tenant;
    }

}
