package io.slingr.endpoints.veritoneuser;

import io.slingr.endpoints.HttpPerUserEndpoint;
import io.slingr.endpoints.exceptions.EndpointException;
import io.slingr.endpoints.framework.annotations.*;
import io.slingr.endpoints.exceptions.ErrorCode;
import io.slingr.endpoints.services.AppLogs;
import io.slingr.endpoints.services.exchange.ReservedName;
import io.slingr.endpoints.services.rest.RestClient;
import io.slingr.endpoints.services.rest.RestMethod;
import io.slingr.endpoints.utils.Json;
import io.slingr.endpoints.ws.exchange.FunctionRequest;
import io.slingr.endpoints.ws.exchange.WebServiceRequest;
import io.slingr.endpoints.ws.exchange.WebServiceResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;


@SlingrEndpoint(name = "veritoneuser", functionPrefix = "_")
public class VeritoneUserEndpoint extends HttpPerUserEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(VeritoneUserEndpoint.class);

    @ApplicationLogger
    protected AppLogs appLogger;

    @EndpointProperty
    private String environment;

    @EndpointProperty
    private String region;

    @EndpointProperty
    private String clientId;

    @EndpointProperty
    private String clientSecret;

    @EndpointProperty
    private String redirectUri;

    public VeritoneUserEndpoint() { }

    @Override
    public String getApiUri() {
        String prefix = "https://api";
        String postfix = ".veritone.com";
        String apiUri="";

        if (Objects.equals(environment, "dev")) {
            apiUri = ".dev";
        } else if (Objects.equals(environment, "stage")) {
            apiUri = ".stage";
        }

        if (Objects.equals(region, "us-1")) {
            apiUri = apiUri + ".us-1";
        } else if (Objects.equals(region, "ca-1")) {
            apiUri = apiUri + ".ca-1";
        } else if (Objects.equals(region, "uk-1")) {
            apiUri = apiUri + ".uk-1";
        } else {
            apiUri = apiUri + region;
        }

        return prefix + apiUri + postfix;
    }

    @Override
    public void endpointStarted() {
        httpService().setAllowExternalUrl(true);
    }

    // Authentication process
    @EndpointWebService(path = "authCallback")
    public WebServiceResponse authCallback(WebServiceRequest request) {
        Json parameters = request.getParameters();
        String userId="";

        if (parameters != null && StringUtils.isNotBlank(parameters.string("code"))) {
            String code = parameters.string("code");
            Json accessTokenRequest = Json.map()
                    .set("headers", Json.map().set("Content-Type", "application/x-www-form-urlencoded"))
                    .set("body", Json.map()
                            .set("client_id", clientId)
                            .set("client_secret", clientSecret)
                            .set("code", code)
                            .set("redirect_uri", redirectUri)
                            .set("grant_type", "authorization_code")
                    );
            Json res = RestClient.builder(getApiUri().concat("/v1/admin/oauth/token")).httpPost(accessTokenRequest);
            if (res.contains("access_token")) {
                //return setUserConnected(request, userId, res);
            } else {
                logger.warn(String.format("Problems trying to connect user [%s] to Veritone: %s", userId, res));
                appLogger.warn(String.format("Problems trying to connect user [%s] to Veritone %s", userId, res.string("error")));
            }
        } else {
            logger.info(String.format("Empty 'code' when try to connect user [%s] [%s]", userId, request.toString()));
        }
        return new WebServiceResponse();
    }

    @EndpointFunction(name = ReservedName.CONNECT_USER)
    public Json connectUser(FunctionRequest request) {
        final String userId = request.getUserId();
        if (StringUtils.isNotBlank(userId)) {
            String accessToken = request.getJsonParams().string("accessToken");
            String refreshToken = request.getJsonParams().string("refreshToken");
            if (StringUtils.isNotBlank(accessToken) && StringUtils.isNotBlank(refreshToken)) {
                Json userConf = Json.map();
                userConf.set("access_token", accessToken);
                userConf.set("refresh_token", refreshToken);
                return setUserConnected(request, userId, userConf);
            } else {
                return accessTokenRequest(request, userId);
            }
        }
        defaultMethodDisconnectUsers(request);
        return Json.map();
    }

    private Json setUserConnected(FunctionRequest request, String userId, Json res) {
        Json conf = users().save(userId, res);
        logger.info(String.format("User connected [%s] [%s]", userId, conf.toString()));
        users().sendUserConnectedEvent(request.getFunctionId(), userId, conf);
        return conf;
    }

    private Json accessTokenRequest(FunctionRequest request, String userId) {
        // checks if the user includes a non-empty 'code' on the request
        final Json jsonBody = request.getJsonParams();
        if (jsonBody != null && StringUtils.isNotBlank(jsonBody.string("code"))) {
            String code = jsonBody.string("code");
            Json accessTokenRequest = Json.map()
                    .set("headers", Json.map().set("Content-Type", "application/x-www-form-urlencoded"))
                    .set("body", Json.map()
                            .set("client_id", clientId)
                            .set("client_secret", clientSecret)
                            .set("code", code)
                            .set("redirect_uri", jsonBody.string("redirectUri"))
                            .set("grant_type", "authorization_code")
                    );
            Json res = RestClient.builder(getApiUri().concat("/v1/admin/oauth/token")).httpPost(accessTokenRequest);
            if (res.contains("access_token")) {
                return setUserConnected(request, userId, res);
            } else {
                logger.warn(String.format("Problems trying to connect user [%s] to Veritone: %s", userId, res.toString()));
                appLogger.warn(String.format("Problems trying to connect user [%s] to Veritone %s", userId, res.string("error")));
            }
        } else {
            logger.info(String.format("Empty 'code' when try to connect user [%s] [%s]", userId, request.toString()));
        }
        return Json.map();
    }

    @EndpointFunction(name = "_post")
    public Json post(FunctionRequest request) {
        try {
            setUserRequestHeaders(request);
            return defaultPostRequest(request);
        } catch (EndpointException restException) {
            if (restException.getHttpStatusCode() == 401) {
                // we might need to refresh the token
                generateNewAccessToken(request);
                setUserRequestHeaders(request);
                return defaultPostRequest(request);
            } else if (restException.getCode() == ErrorCode.CLIENT) {
                users().sendUserDisconnectedEvent(request.getUserId());
            }
            throw restException;
        }
    }

    @EndpointWebService(methods = {RestMethod.POST})
    private WebServiceResponse inboundEvent(WebServiceRequest request) {
        events().send("webhook", request.getJsonBody());
        return new WebServiceResponse();
    }

    private void setUserRequestHeaders(FunctionRequest request) {
        Json userConfig = users().findById(request.getUserId());
        if (userConfig == null || userConfig.isEmpty("access_token")) {
            throw EndpointException.permanent(ErrorCode.CLIENT, String.format("User [%s] is not connected", request.getUserEmail()));
        }
        Json body = request.getJsonParams();
        String token = userConfig.string("access_token");
        Json headers = body.json("headers");
        if (headers == null) {
            headers = Json.map();
        }
        headers.set("Authorization", "Bearer " + token);
        headers.set("Content-Type", "application/json");
        body.set("headers", headers);
        request.getRequest().set("params", body);
    }

    private void generateNewAccessToken(FunctionRequest request) {
        final String userId = request.getUserId();
        Json userConfig = users().findById(userId);
        if (userConfig == null || userConfig.isEmpty("refresh_token")) {
            throw EndpointException.permanent(ErrorCode.CLIENT, String.format("User [%s] is not connected", request.getUserEmail()));
        }
        String refreshToken = userConfig.string("refresh_token");
        Json accessTokenRequest = Json.map()
                .set("headers", Json.map().set("Content-Type", "application/x-www-form-urlencoded"))
                .set("body", Json.map()
                        .set("client_id", clientId)
                        .set("client_secret", clientSecret)
                        .set("grant_type", "refresh_token")
                        .set("refresh_token", refreshToken)
                );
        try {
            Json res = RestClient.builder(getApiUri().concat("/v1/admin/oauth/token")).httpPost(accessTokenRequest);
            if (res.contains("access_token")) {
                setUserConnected(request, userId, res);
            } else {
                logger.warn(String.format("Problems trying to connect user [%s] to Veritone: %s", userId, res.toString()));
                appLogger.warn(String.format("Problems trying to connect user [%s] to Veritone %s", userId, res.string("error")));
            }
        } catch (Exception e) {
            appLogger.error(String.format("Error refreshing token for client ID [%s]. You might need to get a new refresh token.", clientId));
            throw e;
        }
    }
}