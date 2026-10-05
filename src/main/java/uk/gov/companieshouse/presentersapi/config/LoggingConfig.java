package uk.gov.companieshouse.presentersapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Configuration
@PropertySource("classpath:logger.properties")
public class LoggingConfig {

    @Bean
    public Logger logger(@Value("${logger.namespace}") String loggerNamespace) {
        return LoggerFactory.getLogger(loggerNamespace);
    }
}
