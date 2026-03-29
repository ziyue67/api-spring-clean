package com.youthnightschool.config;

import com.youthnightschool.interceptor.ApiVersionInterceptor;
import com.youthnightschool.interceptor.LoggingInterceptor;
import com.youthnightschool.interceptor.PerformanceInterceptor;
import com.youthnightschool.interceptor.RateLimiterInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration. Registers interceptors and sets request timeout
 * from application properties.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  private final LoggingInterceptor loggingInterceptor;
  private final PerformanceInterceptor performanceInterceptor;
  private final RateLimiterInterceptor rateLimiterInterceptor;
  private final ApiVersionInterceptor apiVersionInterceptor;

  public WebMvcConfig(
      LoggingInterceptor loggingInterceptor,
      PerformanceInterceptor performanceInterceptor,
      RateLimiterInterceptor rateLimiterInterceptor,
      ApiVersionInterceptor apiVersionInterceptor) {
    this.loggingInterceptor = loggingInterceptor;
    this.performanceInterceptor = performanceInterceptor;
    this.rateLimiterInterceptor = rateLimiterInterceptor;
    this.apiVersionInterceptor = apiVersionInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(loggingInterceptor);
    registry.addInterceptor(performanceInterceptor);
    registry.addInterceptor(rateLimiterInterceptor);
    registry.addInterceptor(apiVersionInterceptor);
  }
}
