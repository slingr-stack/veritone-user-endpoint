endpoint.gql = function(query, variables) {
    return endpoint.user.post('/v3/graphql', {
        query: query,
        variables: variables
    });
};

/////////////////////
// Public API - Generic Functions
/////////////////////

endpoint.get = function (url) {
    options = checkHttpOptions(url, {});
    return endpoint._getRequest(options);
};

endpoint.post = function (url, options) {
    options = checkHttpOptions(url, options);
    return endpoint._postRequest(options);
};

endpoint.put = function (url, options) {
    options = checkHttpOptions(url, options);
    return endpoint._putRequest(options);
};

endpoint.patch = function (url, options) {
    options = checkHttpOptions(url, options);
    return endpoint._patchRequest(options);
};

endpoint.delete = function (url) {
    var options = checkHttpOptions(url, {});
    return endpoint._deleteRequest(options);
};