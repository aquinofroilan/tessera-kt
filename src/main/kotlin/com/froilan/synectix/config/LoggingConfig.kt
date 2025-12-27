package com.froilan.synectix.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy

@Configuration
@EnableAspectJAutoProxy
class LoggingConfig {
    // This configuration enables AOP and AspectJ auto proxy creation
    // The @EnableAspectJAutoProxy annotation tells Spring to look for
    // @Aspect annotated classes and create proxies for them
}
