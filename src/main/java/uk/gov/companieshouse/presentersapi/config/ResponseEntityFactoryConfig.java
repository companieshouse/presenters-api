package uk.gov.companieshouse.presentersapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.companieshouse.presentersapi.response.QueryFoundFactory;
import uk.gov.companieshouse.presentersapi.response.QueryInvalidFactory;
import uk.gov.companieshouse.service.rest.response.PluggableResponseEntityFactory;

@Configuration
public class ResponseEntityFactoryConfig {
    @Bean
    public PluggableResponseEntityFactory createResponseFactory() {
        return PluggableResponseEntityFactory
                   .builder()
                   .addStandardFactories()
                   .add(new QueryFoundFactory())
                   .add(new QueryInvalidFactory())
                   .build();
    }

}
