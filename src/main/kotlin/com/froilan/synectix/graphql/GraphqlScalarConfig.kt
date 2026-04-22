package com.froilan.synectix.graphql

import graphql.language.ArrayValue
import graphql.language.BooleanValue
import graphql.language.EnumValue
import graphql.language.FloatValue
import graphql.language.IntValue
import graphql.language.NullValue
import graphql.language.ObjectField
import graphql.language.ObjectValue
import graphql.language.StringValue
import graphql.language.Value
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
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

        override fun parseLiteral(input: Value<*>): Any = parseLiteralValue(input)

        private fun parseLiteralValue(value: Value<*>): Any =
            when (value) {
                is NullValue -> emptyMap<String, Any>()
                is StringValue -> value.value
                is BooleanValue -> value.isValue
                is IntValue -> value.value
                is FloatValue -> value.value
                is EnumValue -> value.name
                is ArrayValue -> value.values.map { parseLiteralValue(it) }
                is ObjectValue -> value.objectFields.associate(ObjectField::name) { parseLiteralValue(it.value) }
                else -> throw CoercingParseLiteralException("Unsupported JSON literal: ${value.javaClass.simpleName}")
            }

        override fun valueToLiteral(input: Any): Value<*> = throw CoercingSerializeException("JSON scalar does not support valueToLiteral")

        override fun parseValue(input: Any, graphQLContext: graphql.GraphQLContext, locale: java.util.Locale): Any =
            try {
                parseValue(input)
            } catch (e: Exception) {
                throw CoercingParseValueException(e.message ?: "Invalid JSON value")
            }

        override fun parseLiteral(
            input: Value<*>,
            variables: MutableMap<String, Any>,
            graphQLContext: graphql.GraphQLContext,
            locale: java.util.Locale,
        ): Any = parseLiteral(input)

        override fun serialize(input: Any, graphQLContext: graphql.GraphQLContext, locale: java.util.Locale): Any =
            try {
                serialize(input)
            } catch (e: Exception) {
                throw CoercingSerializeException(e.message ?: "Unable to serialize JSON value")
            }
    }
}
