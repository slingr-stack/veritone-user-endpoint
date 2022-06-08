
endpoint.user = endpoint.user || {};

endpoint.user.userByName = function(id) {
    var vars = {
        "param": id
    };

    var query = `query ($param: ID!) {
                      application(id: $param) {
                         name
                      }
                    }`;
    return endpoint.query(query, vars);
};
