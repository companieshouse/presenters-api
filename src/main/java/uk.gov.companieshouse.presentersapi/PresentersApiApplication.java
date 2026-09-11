package uk.gov.companieshouse.presentersapi;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PresentersApiApplication {

    public static final String APP_NAMESPACE = "presenters-api";

    public static void main(String[] args) { SpringApplication.run(PresentersApiApplication.class, args); }
}
