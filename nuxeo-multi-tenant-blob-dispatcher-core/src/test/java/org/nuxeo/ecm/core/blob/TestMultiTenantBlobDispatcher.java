/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.blob;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.multi.tenant.MultiTenantBlobDispatcher;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
public class TestMultiTenantBlobDispatcher {

    protected static final String DEFAULT = "default";

    protected static final String CUSTOM = "custom";

    protected MultiTenantBlobDispatcher dispatcherWith(String clause) {
        MultiTenantBlobDispatcher dispatcher = new MultiTenantBlobDispatcher();
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put(clause, CUSTOM);
        properties.put("default", DEFAULT); // NOSONAR
        dispatcher.initialize(properties);
        return dispatcher;
    }

    protected void expect(MultiTenantBlobDispatcher dispatcher, String expected, Object value) {
        Document doc = mock(Document.class);
        when(doc.getValue("prop")).thenReturn(value);
        assertEquals(expected, dispatcher.getProviderId(doc, null, null));
    }

    // ===== Null =====

    @Test
    public void testOperatorNullEq() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("prop=null");
        expect(dispatcher, CUSTOM, null);
        expect(dispatcher, DEFAULT, "foo");
        expect(dispatcher, DEFAULT, 555L);
    }

    @Test
    public void testOperatorNullNeq() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("prop!=null");
        expect(dispatcher, DEFAULT, null);
        expect(dispatcher, CUSTOM, "foo");
        expect(dispatcher, CUSTOM, 555L);
    }

    // ===== String =====

    @Test
    public void testOperatorStringEq() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("prop=foo");
        expect(dispatcher, DEFAULT, "bar");
        expect(dispatcher, CUSTOM, "foo");
    }

    @Test
    public void testOperatorStringNeq() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("prop!=foo");
        expect(dispatcher, CUSTOM, "bar");
        expect(dispatcher, DEFAULT, "foo");
    }

    @Test
    public void testOperatorStringLt() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("prop<foo");
        expect(dispatcher, CUSTOM, null);
        expect(dispatcher, CUSTOM, "bar");
        expect(dispatcher, DEFAULT, "foo");
        expect(dispatcher, DEFAULT, "gee");
    }

    @Test
    public void testOperatorStringLte() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("prop<=foo");
        expect(dispatcher, CUSTOM, null);
        expect(dispatcher, CUSTOM, "bar");
        expect(dispatcher, CUSTOM, "foo");
        expect(dispatcher, DEFAULT, "gee");
    }

    @Test
    public void testOperatorStringGt() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("prop>foo");
        expect(dispatcher, DEFAULT, null);
        expect(dispatcher, DEFAULT, "bar");
        expect(dispatcher, DEFAULT, "foo");
        expect(dispatcher, CUSTOM, "gee");
    }

    @Test
    public void testOperatorStringGte() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("prop>=foo");
        expect(dispatcher, DEFAULT, null);
        expect(dispatcher, DEFAULT, "bar");
        expect(dispatcher, CUSTOM, "foo");
        expect(dispatcher, CUSTOM, "gee");
    }

    @Test
    public void testOperatorStringGlob() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("prop~/fo?/bar*");
        expect(dispatcher, DEFAULT, null);
        expect(dispatcher, DEFAULT, "gee");
        expect(dispatcher, DEFAULT, "foo/bar");
        expect(dispatcher, DEFAULT, "/foo/ba");
        expect(dispatcher, DEFAULT, "/foo/baz");
        expect(dispatcher, CUSTOM, "/foo/bar");
        expect(dispatcher, CUSTOM, "/foo/bar/gee");
        expect(dispatcher, CUSTOM, "/fox/bar");
    }

    @Test
    public void testOperatorStringRegexp() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("prop^[fg]oo/?");
        expect(dispatcher, DEFAULT, null);
        expect(dispatcher, DEFAULT, "gee");
        expect(dispatcher, CUSTOM, "foo");
        expect(dispatcher, CUSTOM, "goo");
        expect(dispatcher, CUSTOM, "foo/");
        expect(dispatcher, CUSTOM, "goo/");
    }

    // ===== Boolean =====

    @Test
    public void testOperatorBooleanEq() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("prop=true");
        expect(dispatcher, DEFAULT, false);
        expect(dispatcher, CUSTOM, true);
        dispatcher = dispatcherWith("prop=foo");
        expect(dispatcher, DEFAULT, false);
        expect(dispatcher, DEFAULT, true);
    }

    @Test
    public void testOperatorBooleanNeq() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("prop!=true");
        expect(dispatcher, CUSTOM, false);
        expect(dispatcher, DEFAULT, true);
        dispatcher = dispatcherWith("prop!=foo");
        expect(dispatcher, CUSTOM, false);
        expect(dispatcher, CUSTOM, true);
    }

    // ===== Long =====

    @Test
    public void testOperatorLongEq() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("prop=555");
        expect(dispatcher, DEFAULT, 9L);
        expect(dispatcher, CUSTOM, 555L);
        dispatcher = dispatcherWith("prop=foo");
        expect(dispatcher, DEFAULT, 555L);
    }

    @Test
    public void testOperatorLongNeq() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("prop!=555");
        expect(dispatcher, CUSTOM, 9L);
        expect(dispatcher, DEFAULT, 555L);
        dispatcher = dispatcherWith("prop!=foo");
        expect(dispatcher, CUSTOM, 555L);
    }

    @Test
    public void testOperatorLongLt() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("prop<555");
        expect(dispatcher, CUSTOM, null); // treated as 0
        expect(dispatcher, CUSTOM, 9L); // to be sure we don't compare as strings
        expect(dispatcher, DEFAULT, 555L);
        expect(dispatcher, DEFAULT, 987L);
        dispatcher = dispatcherWith("prop<foo");
        expect(dispatcher, DEFAULT, 555L);
    }

    @Test
    public void testOperatorLongLte() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("prop<=555");
        expect(dispatcher, CUSTOM, null); // treated as 0
        expect(dispatcher, CUSTOM, 9L);
        expect(dispatcher, CUSTOM, 555L);
        expect(dispatcher, DEFAULT, 987L);
        dispatcher = dispatcherWith("prop<=foo");
        expect(dispatcher, DEFAULT, 555L);
    }

    @Test
    public void testOperatorLongGt() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("prop>555");
        expect(dispatcher, DEFAULT, null); // treated as 0
        expect(dispatcher, DEFAULT, 9L);
        expect(dispatcher, DEFAULT, 555L);
        expect(dispatcher, CUSTOM, 987L);
        dispatcher = dispatcherWith("prop>foo");
        expect(dispatcher, DEFAULT, 555L);
    }

    @Test
    public void testOperatorLongGte() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("prop>=555");
        expect(dispatcher, DEFAULT, null); // treated as 0
        expect(dispatcher, DEFAULT, 9L);
        expect(dispatcher, CUSTOM, 555L);
        expect(dispatcher, CUSTOM, 987L);
        dispatcher = dispatcherWith("prop>=foo");
        expect(dispatcher, DEFAULT, 555L);
    }

    // ===== Double =====

    @Test
    public void testOperatorDoubleEq() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("prop=555");
        expect(dispatcher, DEFAULT, 9.0D);
        expect(dispatcher, CUSTOM, 555.0D);
        dispatcher = dispatcherWith("prop=foo");
        expect(dispatcher, DEFAULT, 555.0D);
    }

    @Test
    public void testOperatorDoubleNeq() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("prop!=555");
        expect(dispatcher, CUSTOM, 9.0D);
        expect(dispatcher, DEFAULT, 555.0D);
        dispatcher = dispatcherWith("prop!=foo");
        expect(dispatcher, CUSTOM, 555.0D);
    }

    @Test
    public void testOperatorDoubleLt() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("prop<555");
        expect(dispatcher, CUSTOM, 9.0D); // to be sure we don't compare as strings
        expect(dispatcher, DEFAULT, 555.0D);
        expect(dispatcher, DEFAULT, 987.0D);
        dispatcher = dispatcherWith("prop<foo");
        expect(dispatcher, DEFAULT, 555.0D);
    }

    @Test
    public void testOperatorDoubleLte() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("prop<=555");
        expect(dispatcher, CUSTOM, 9.0D);
        expect(dispatcher, CUSTOM, 555.0D);
        expect(dispatcher, DEFAULT, 987.0D);
        dispatcher = dispatcherWith("prop<=foo");
        expect(dispatcher, DEFAULT, 555.0D);
    }

    @Test
    public void testOperatorDoubleGt() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("prop>555");
        expect(dispatcher, DEFAULT, 9.0D);
        expect(dispatcher, DEFAULT, 555.0D);
        expect(dispatcher, CUSTOM, 987.0D);
        dispatcher = dispatcherWith("prop>foo");
        expect(dispatcher, DEFAULT, 555.0D);
    }

    @Test
    public void testOperatorDoubleGte() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("prop>=555");
        expect(dispatcher, DEFAULT, 9.0D);
        expect(dispatcher, CUSTOM, 555.0D);
        expect(dispatcher, CUSTOM, 987.0D);
        dispatcher = dispatcherWith("prop>=foo");
        expect(dispatcher, DEFAULT, 555.0D);
    }

    // ===== Calendar =====

    protected static Calendar cal(String string) {
        ZonedDateTime instant = ZonedDateTime.ofInstant(Instant.parse(string), ZoneOffset.UTC);
        return GregorianCalendar.from(instant);
    }

    protected static final Calendar CAL1 = cal("2020-01-02T03:04:05Z");

    protected static final Calendar CAL2 = cal("2020-01-02T12:34:56Z");

    protected static final Calendar CAL3 = cal("2050-12-25T00:00:00Z");

    @Test
    public void testOperatorCalendarGlob() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("prop~2020-01-*");
        expect(dispatcher, CUSTOM, CAL1);
        expect(dispatcher, CUSTOM, CAL2);
        expect(dispatcher, DEFAULT, CAL3);
    }

    @Test
    public void testOperatorCalendarRegexp() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("prop^2020-01-.*");
        expect(dispatcher, CUSTOM, CAL1);
        expect(dispatcher, CUSTOM, CAL2);
        expect(dispatcher, DEFAULT, CAL3);
    }

    @Test
    public void testOperatorCalendarEq() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("prop=2020-01-02T12:34:56Z");
        expect(dispatcher, DEFAULT, CAL1);
        expect(dispatcher, CUSTOM, CAL2);
        dispatcher = dispatcherWith("prop=foo");
        expect(dispatcher, DEFAULT, CAL1);
    }

    @Test
    public void testOperatorCalendarNeq() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("prop!=2020-01-02T12:34:56Z");
        expect(dispatcher, CUSTOM, CAL1);
        expect(dispatcher, DEFAULT, CAL2);
        dispatcher = dispatcherWith("prop!=foo");
        expect(dispatcher, CUSTOM, CAL1);
    }

    @Test
    public void testOperatorCalendarLt() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("prop<2020-01-02T12:34:56Z");
        expect(dispatcher, CUSTOM, CAL1);
        expect(dispatcher, DEFAULT, CAL2);
        expect(dispatcher, DEFAULT, CAL3);
        dispatcher = dispatcherWith("prop<foo");
        expect(dispatcher, DEFAULT, CAL1);
    }

    @Test
    public void testOperatorCalendarLte() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("prop<=2020-01-02T12:34:56Z");
        expect(dispatcher, CUSTOM, CAL1);
        expect(dispatcher, CUSTOM, CAL2);
        expect(dispatcher, DEFAULT, CAL3);
        dispatcher = dispatcherWith("prop<=foo");
        expect(dispatcher, DEFAULT, CAL1);
    }

    @Test
    public void testOperatorCalendarGt() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("prop>2020-01-02T12:34:56Z");
        expect(dispatcher, DEFAULT, CAL1);
        expect(dispatcher, DEFAULT, CAL2);
        expect(dispatcher, CUSTOM, CAL3);
        dispatcher = dispatcherWith("prop>foo");
        expect(dispatcher, DEFAULT, CAL1);
    }

    @Test
    public void testOperatorCalendarGte() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("prop>=2020-01-02T12:34:56Z");
        expect(dispatcher, DEFAULT, CAL1);
        expect(dispatcher, CUSTOM, CAL2);
        expect(dispatcher, CUSTOM, CAL3);
        dispatcher = dispatcherWith("prop>=foo");
        expect(dispatcher, DEFAULT, CAL1);
    }

    // ===== Clauses =====

    @Test
    public void testClausesAnded() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("prop1=foo,prop2=bar");
        Document doc = mock(Document.class);

        when(doc.getValue("prop1")).thenReturn("foo");
        when(doc.getValue("prop2")).thenReturn("bar");
        assertEquals(CUSTOM, dispatcher.getProviderId(doc, null, null));

        when(doc.getValue("prop1")).thenReturn("foo");
        when(doc.getValue("prop2")).thenReturn("gee");
        assertEquals(DEFAULT, dispatcher.getProviderId(doc, null, null));

        when(doc.getValue("prop1")).thenReturn("gee");
        when(doc.getValue("prop2")).thenReturn("bar");
        assertEquals(DEFAULT, dispatcher.getProviderId(doc, null, null));
    }

    // ===== Names =====

    @Test
    public void testNameBlobName() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("blob:name=foo");
        Blob blob = mock(Blob.class);

        when(blob.getFilename()).thenReturn("bar");
        assertEquals(DEFAULT, dispatcher.getProviderId(null, blob, null));

        when(blob.getFilename()).thenReturn("foo");
        assertEquals(CUSTOM, dispatcher.getProviderId(null, blob, null));
    }

    @Test
    public void testNameBlobMimeType() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("blob:mime-type=foo");
        Blob blob = mock(Blob.class);

        when(blob.getMimeType()).thenReturn("bar");
        assertEquals(DEFAULT, dispatcher.getProviderId(null, blob, null));

        when(blob.getMimeType()).thenReturn("foo");
        assertEquals(CUSTOM, dispatcher.getProviderId(null, blob, null));
    }

    @Test
    public void testNameBlobEncoding() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("blob:encoding=foo");
        Blob blob = mock(Blob.class);

        when(blob.getEncoding()).thenReturn("bar");
        assertEquals(DEFAULT, dispatcher.getProviderId(null, blob, null));

        when(blob.getEncoding()).thenReturn("foo");
        assertEquals(CUSTOM, dispatcher.getProviderId(null, blob, null));
    }

    @Test
    public void testNameBlobDigest() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("blob:digest=foo");
        Blob blob = mock(Blob.class);

        when(blob.getDigest()).thenReturn("bar");
        assertEquals(DEFAULT, dispatcher.getProviderId(null, blob, null));

        when(blob.getDigest()).thenReturn("foo");
        assertEquals(CUSTOM, dispatcher.getProviderId(null, blob, null));
    }

    @Test
    public void testNameBlobLength() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("blob:length>500");
        Blob blob = mock(Blob.class);

        when(blob.getLength()).thenReturn(9L);
        assertEquals(DEFAULT, dispatcher.getProviderId(null, blob, null));

        when(blob.getLength()).thenReturn(555L);
        assertEquals(CUSTOM, dispatcher.getProviderId(null, blob, null));
    }

    @Test
    public void testNameBlobXpath() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("blob:xpath=foo");

        assertEquals(DEFAULT, dispatcher.getProviderId(null, null, "bar"));

        assertEquals(CUSTOM, dispatcher.getProviderId(null, null, "foo"));
    }

    @Test
    public void testNameEcmRepositoryName() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("ecm:repositoryName=foo");
        Document doc = mock(Document.class);

        when(doc.getRepositoryName()).thenReturn("bar");
        assertEquals(DEFAULT, dispatcher.getProviderId(doc, null, null));

        when(doc.getRepositoryName()).thenReturn("foo");
        assertEquals(CUSTOM, dispatcher.getProviderId(doc, null, null));
    }

    @Test
    public void testNameEcmPath() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("ecm:path=/foo");
        Document doc = mock(Document.class);

        when(doc.getPath()).thenReturn("/bar");
        assertEquals(DEFAULT, dispatcher.getProviderId(doc, null, null));

        when(doc.getPath()).thenReturn("/foo");
        assertEquals(CUSTOM, dispatcher.getProviderId(doc, null, null));
    }

    @Test
    public void testNameIsRecord() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("ecm:isRecord=true");
        Document doc = mock(Document.class);

        when(doc.isRecord()).thenReturn(false);
        assertEquals(DEFAULT, dispatcher.getProviderId(doc, null, null));

        when(doc.isRecord()).thenReturn(true);
        assertEquals(CUSTOM, dispatcher.getProviderId(doc, null, null));
    }

    @Test
    public void testNameRecords() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("records");
        Document doc = mock(Document.class);

        when(doc.isRecord()).thenReturn(false);
        assertEquals(DEFAULT, dispatcher.getProviderId(doc, null, null));

        when(doc.isRecord()).thenReturn(true);
        assertEquals(DEFAULT, dispatcher.getProviderId(doc, null, null));

        when(doc.isRecord()).thenReturn(true);
        assertEquals(CUSTOM, dispatcher.getProviderId(doc, null, "content"));
    }

    @Test
    public void testMultiTenant() {
        MultiTenantBlobDispatcher dispatcher = dispatcherWith("ecm:tenant=test");
        Document doc = mock(Document.class);

        when(doc.hasFacet("TenantConfig")).thenReturn(true);
        when(doc.getValue("tenantconfig:tenantId")).thenReturn("test");
        assertEquals(CUSTOM, dispatcher.getProviderId(doc, null, null));
    }

}