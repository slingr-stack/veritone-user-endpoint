package io.slingr.endpoints.veritoneuser;

import io.slingr.endpoints.HttpPerUserEndpoint;
import io.slingr.endpoints.exceptions.EndpointException;
import io.slingr.endpoints.framework.annotations.*;
import io.slingr.endpoints.exceptions.ErrorCode;
import io.slingr.endpoints.services.AppLogs;
import io.slingr.endpoints.services.exchange.ReservedName;
import io.slingr.endpoints.services.datastores.DataStore;
import io.slingr.endpoints.utils.Json;
import io.slingr.endpoints.ws.exchange.FunctionRequest;
import io.slingr.endpoints.ws.exchange.WebServiceRequest;
import io.slingr.endpoints.ws.exchange.WebServiceResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SlingrEndpoint(name = "veritone-user")
public class VeritoneUserEndpoint extends HttpPerUserEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(VeritoneUserEndpoint.class);

    @EndpointProperty
    private String environment;

    @ApplicationLogger
    protected AppLogs appLogger;

    @EndpointProperty
    private String clientId;

    @EndpointProperty
    private String clientSecret;

    @EndpointProperty
    private String redirectUri;

    public VeritoneUserEndpoint() {
    }

    @Override
    public String getApiUri() {
        if (environment.equals("production")) {
            return "https://api.veritone.com";
        } else {
            return "https://api.stage.veritone.com";
        }
    }

    @Override
    public void endpointStarted() {
    }

    // Authentication process

    @EndpointWebService(path = "authCallback")
    public WebServiceResponse authCallback() {
        return new WebServiceResponse("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n" +
                "<html>\n" +
                "<head>\n" +
                "<title>Veritone authentication</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "</body>\n" +
                "</html>", ContentType.TEXT_HTML.toString());
    }

    @EndpointFunction(name = ReservedName.CONNECT_USER)
    public Json connectUser(FunctionRequest request) {
        final String userId = request.getUserId();
        if(StringUtils.isNotBlank(userId)) {
            // checks if the user includes a non-empty 'code' on the request
            final Json jsonBody = request.getJsonParams();
            if (jsonBody != null && StringUtils.isNotBlank(jsonBody.string("code"))) {
                String code = jsonBody.string("code");
                Json accessTokenRequest = Json.map()
                        .set("path", getApiUri() + "/v1/admin/oauth/token")
                        .set("headers", Json.map().set("Content-Type", "application/x-www-form-urlencoded"))
                        .set("body", Json.map()
                                .set("client_id", clientId)
                                .set("client_secret", clientSecret)
                                .set("code", code)
                                .set("redirect_uri", jsonBody.string("redirectUri"))
                                .set("grant_type", "authorization_code")
                        );
                Json res = httpService().defaultPostRequest(accessTokenRequest);
                if (res.contains("access_token")) {
                    // saves the information on the users data store
                    Json conf = users().save(userId, res);
                    logger.info(String.format("User connected [%s] [%s]", userId, conf.toString()));

                    // sends connected user event
                    users().sendUserConnectedEvent(request.getFunctionId(), userId, conf);

                    return conf;
                } else {
                    logger.warn(String.format("Problems trying to connect user [%s] to Veritone: %s", userId, res.toString()));
                    appLogger.warn(String.format("Problems trying to connect user [%s] to Veritone %s", userId, res.string("error")));
                }
            } else {
                logger.info(String.format("Empty 'code' when try to connect user [%s] [%s]", userId, request.toString()));
            }
        }
        defaultMethodDisconnectUsers(request);
        return Json.map();
    }



    // Internal methods

    @EndpointFunction(name = "_userGet")
    public Json userGet(FunctionRequest request) {
        try {
            //setUserRequestHeaders(request);
            Json res = defaultGetRequest(request);
            return res;
        } catch (EndpointException restException) {
            if (restException.getCode() == ErrorCode.CLIENT) {
                users().sendUserDisconnectedEvent(request.getUserId());
            }
            throw restException;
        }
    }
}