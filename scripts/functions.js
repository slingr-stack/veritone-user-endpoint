endpoint.query = function(query, variables) {
    return endpoint.user.post('/v3/graphql', {
        query: query,
        variables: variables
    });
};

endpoint.mutation = function(mutation, variables) {
    return endpoint.user.post('/v3/graphql', {
        mutation: mutation,
        variables: variables
    });
};