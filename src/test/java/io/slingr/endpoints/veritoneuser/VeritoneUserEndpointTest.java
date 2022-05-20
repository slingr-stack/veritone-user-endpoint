package io.slingr.endpoints.veritoneuser;

import io.slingr.endpoints.utils.Json;
import io.slingr.endpoints.utils.tests.EndpointTests;
import io.slingr.endpoints.ws.exchange.FunctionRequest;

import io.slingr.endpoints.ws.exchange.WebServiceResponse;
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
    public void testConnectUser() {

    }
}
