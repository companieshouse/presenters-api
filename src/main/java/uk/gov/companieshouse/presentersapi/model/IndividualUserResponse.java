package uk.gov.companieshouse.presentersapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IndividualUserResponse(
        @JsonProperty("individual_user") IndividualUser individualUser) {

    public record IndividualUser(
            @JsonProperty("UVID") String uvid,
            String name,
            String dob,
            String email) {
    }
}
