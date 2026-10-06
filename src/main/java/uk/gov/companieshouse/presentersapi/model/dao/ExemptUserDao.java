package uk.gov.companieshouse.presentersapi.model.dao;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "exempt_users")
public class ExemptUserDao {

    private static final String EMAIL_FIELD_NAME = "email";
    private static final String ORGANISATION_FIELD_NAME = "organisation";
    private static final String CREATED_AT_FIELD_NAME = "created_at";
    private static final String UPDATED_AT_FIELD_NAME = "updated_at";
    private static final String FORENAME_FIELD_NAME = "forename";
    private static final String SURNAME_FIELD_NAME = "surname";
    private static final String EXEMPT_PRESENTER_STATEMENT_FIELD_NAME = "exempt_presenter_statement";

    @Id
    private String id;

    @Field(EMAIL_FIELD_NAME)
    private String email;

    @Field(ORGANISATION_FIELD_NAME)
    private String organisation;

    @Field(CREATED_AT_FIELD_NAME)
    private LocalDateTime createdAt;

    @Field(UPDATED_AT_FIELD_NAME)
    private LocalDateTime updatedAt;

    @Field(FORENAME_FIELD_NAME)
    private String forename;

    @Field(SURNAME_FIELD_NAME)
    private String surname;

    @Field(EXEMPT_PRESENTER_STATEMENT_FIELD_NAME)
    private String exemptPresenterStatement;

    public String getForename(){
        return forename;
    }

    public String getSurname(){
        return surname;
    }

    public String getFullName(){
        return forename+" "+surname;
    }

    public String getEmail() {
        return email;
    }

    public String getId(){
        return id;
    }

    public String getExemptPresenterStatement() {
        return exemptPresenterStatement;
    }

    public String getOrganisation() {
        return organisation;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setEmail(String email){
        this.email = email;
    }

    public void setId(String id){
        this.id = id;
    }
}
