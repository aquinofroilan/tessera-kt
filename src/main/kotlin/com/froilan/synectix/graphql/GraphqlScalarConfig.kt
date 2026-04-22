package com.froilan.synectix.graphql

import graphql.language.ArrayValue
import graphql.language.BooleanValue
import graphql.language.EnumValue
import graphql.language.FloatValue
import graphql.language.IntValue
import graphql.language.NullValue
import graphql.language.ObjectValue
import graphql.language.StringValue
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.GraphQLScalarType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.execution.RuntimeWiringConfigurer

@Configuration
class GraphqlScalarConfig {
    @Bean
    fun jsonScalarConfigurer(): RuntimeWiringConfigurer =
        RuntimeWiringConfigurer { wiringBuilder ->
            wiringBuilder.scalar(jsonScalar())
        }

    private fun jsonScalar(): GraphQLScalarType =
        GraphQLScalarType
            .newScalar()
            .name("JSON")
            .description("Arbitrary JSON value")
            .coercing(JsonCoercing)
            .build()

    private object JsonCoercing : Coercing<Any, Any> {
        override fun serialize(dataFetcherResult: Any): Any = dataFetcherResult

        override fun parseValue(input: Any): Any = input

        override fun parseLiteral(input: Any): Any? = parseLiteralValue(input)

        private fun parseLiteralValue(value: Any): Any? =
            when (value) {
                is NullValue -> null
                is StringValue -> value.value
                is BooleanValue -> value.isValue
                is IntValue -> value.value
                is FloatValue -> value.value
                is EnumValue -> value.name
                is ArrayValue -> value.values.map { parseLiteralValue(it) }
                is ObjectValue -> value.objectFields.associate { field -> field.name to parseLiteralValue(field.value) }
                else -> throw CoercingParseLiteralException("Unsupported JSON literal: ${value.javaClass.simpleName}")
            }
    }
}
