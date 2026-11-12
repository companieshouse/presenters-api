package uk.gov.companieshouse.presentersapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.companieshouse.presentersapi.response.QueryFoundNoStoreFactory;
import uk.gov.companieshouse.presentersapi.response.QueryInvalidNoStoreFactory;
import uk.gov.companieshouse.presentersapi.response.QueryUnprocessableNoStoreFactory;
import uk.gov.companieshouse.service.rest.response.PluggableResponseEntityFactory;

@Configuration
public class ResponseEntityFactoryConfig {
    @Bean
    public PluggableResponseEntityFactory createResponseFactory() {
        return PluggableResponseEntityFactory
                   .builder()
                   .addStandardFactories()
                   .add(new QueryFoundNoStoreFactory())
                   .add(new QueryInvalidNoStoreFactory())
                   .add(new QueryUnprocessableNoStoreFactory())
                   .build();
    }

}
