endpoint.gql = function(query, variables) {
    return endpoint.user.post('/v3/graphql', {
        query: query,
        variables: variables
    });
};