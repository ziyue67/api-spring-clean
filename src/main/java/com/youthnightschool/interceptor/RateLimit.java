package com.youthnightschool.interceptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to configure per-endpoint rate limiting via Bucket4j.
 * Apply to controller methods to override the default 60 req/min limit.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
  int limit() default 60;
  int ttlSeconds() default 60;
}
