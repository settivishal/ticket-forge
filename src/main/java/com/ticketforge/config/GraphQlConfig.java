package com.ticketforge.config;

import graphql.language.StringValue;
import graphql.scalars.ExtendedScalars;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import java.time.Instant;
import java.time.temporal.TemporalAccessor;

@Configuration
public class GraphQlConfig {

    public static final GraphQLScalarType DATE_TIME_SCALAR = GraphQLScalarType.newScalar()
            .name("DateTime")
            .description("An ISO-8601 encoded UTC date time string")
            .coercing(new Coercing<Instant, String>() {
                @Override
                public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
                    if (dataFetcherResult instanceof Instant instant) {
                        return instant.toString();
                    }
                    if (dataFetcherResult instanceof TemporalAccessor temporal) {
                        return Instant.from(temporal).toString();
                    }
                    if (dataFetcherResult instanceof String str) {
                        return str;
                    }
                    throw new CoercingSerializeException("Expected an Instant or ISO date string, got: " + dataFetcherResult);
                }

                @Override
                public Instant parseValue(Object input) throws CoercingParseValueException {
                    if (input instanceof String str) {
                        return Instant.parse(str);
                    }
                    if (input instanceof Instant instant) {
                        return instant;
                    }
                    throw new CoercingParseValueException("Expected an ISO date string: " + input);
                }

                @Override
                public Instant parseLiteral(Object input) throws CoercingParseLiteralException {
                    if (input instanceof StringValue stringValue) {
                        return Instant.parse(stringValue.getValue());
                    }
                    throw new CoercingParseLiteralException("Expected a StringValue: " + input);
                }
            })
            .build();

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .scalar(DATE_TIME_SCALAR)
                .scalar(ExtendedScalars.GraphQLLong);
    }
}
