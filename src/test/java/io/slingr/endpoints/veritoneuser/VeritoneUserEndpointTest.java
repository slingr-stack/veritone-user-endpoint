package io.slingr.endpoints.veritoneuser;

import io.slingr.endpoints.utils.Json;
import io.slingr.endpoints.utils.tests.EndpointTests;
import io.slingr.endpoints.ws.exchange.FunctionRequest;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

@Ignore("For dev purposes")
public class VeritoneUserEndpointTest {

    private static EndpointTests test;
    private static VeritoneUserEndpoint endpoint;

    @BeforeClass
    public static void init() throws Exception {
        test = EndpointTests.start(new io.slingr.endpoints.veritoneuser.Runner(), "test.properties");
        endpoint = (VeritoneUserEndpoint) test.getEndpoint();
        test.clearDataStores();
    }

    @Test
    public void testPost() {
        Json headers =  Json.map();
        headers.set("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiJiNTUwYzM4Ny04MDNmLTQ1OTMtYTczNS03ODM4MzhmZmI2MTEiLCJjb250ZW50QXBwbGljYXRpb25JZCI6ImMzOTY3NDRjLWQzZjUtNDBlZS1hMDQxLTcyOTk4OGUzZDU0MiIsIm9yaWdpbkhvc3QiOiJ2ZXJpdG9uZS5zbGluZ3JzLmlvIiwic2NvcGUiOlt7ImFjdGlvbnMiOlsiaW5nZXN0aW9uOmRlbGV0ZSIsImluZ2VzdGlvbjp1cGRhdGUiLCJpbmdlc3Rpb246cmVhZCIsImluZ2VzdGlvbjpjcmVhdGUiLCJqb2I6Y3JlYXRlIiwiam9iOnJlYWQiLCJqb2I6dXBkYXRlIiwiam9iOmRlbGV0ZSIsInRhc2s6dXBkYXRlIiwicmVjb3JkaW5nOmNyZWF0ZSIsInJlY29yZGluZzpyZWFkIiwicmVjb3JkaW5nOnVwZGF0ZSIsInJlY29yZGluZzpkZWxldGUiLCJyZWNvcmRpbmc6Y2xvbmUiLCJyZXBvcnQ6Y3JlYXRlIiwiYW5hbHl0aWNzOnVzYWdlIiwibWVudGlvbjpjcmVhdGUiLCJtZW50aW9uOnJlYWQiLCJtZW50aW9uOnVwZGF0ZSIsIm1lbnRpb246ZGVsZXRlIiwiY29sbGVjdGlvbjpjcmVhdGUiLCJjb2xsZWN0aW9uOnJlYWQiLCJjb2xsZWN0aW9uOnVwZGF0ZSIsImNvbGxlY3Rpb246ZGVsZXRlIiwiYXNzZXQ6dXJpIl19XSwiZXhwaXJlc19pbiI6NjA0ODAwLCJpYXQiOjE2NTM5MjMyNDIsImV4cCI6MTY1NDUyODA0Miwic3ViIjoib2F1dGgyIiwianRpIjoiMWE4ZDE0ZTYtOTU0Yi00OWM2LThhYzctZjhiYjA3MDEzZDE3In0.kAlhsNPuWwIfQStKO3xefSj_LJX8y88Rj9pbGCLxX-k");
        headers.set("Content-Type", "application/json");

        Json query = Json.map();
        query.set("query", "{me {id name}}");

        Json body = Json.map();
        body.set("headers", headers);
        body.set("path", "/v3/graphql");
        body.set("body", query);

        Json request = Json.map();
        request.set("params", body);
        Json res = endpoint.defaultPostRequest(new FunctionRequest(request));
        assertNotNull(res);
        Json data = res.json("data");
        assertNotNull(data);
        Json me = data.json("me");
        assertNotNull(me);
        assertEquals("b550c387-803f-4593-a735-783838ffb611", me.string("id"));
        assertEquals("jorbegozo@slingr.io", me.string("name"));
    }
}
