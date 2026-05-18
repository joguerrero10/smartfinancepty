package com.smartfinancepty.finance.graphql.config;

import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;

public class GraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {
    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        if (ex instanceof IllegalArgumentException) {
            return GraphqlErrorBuilder.newError(env).message(ex.getMessage())
                    .errorType(ErrorType.BAD_REQUEST).build();
        }
        return null;
    }
}
