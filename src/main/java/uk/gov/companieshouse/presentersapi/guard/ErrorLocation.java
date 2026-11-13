package uk.gov.companieshouse.presentersapi.guard;

import java.util.function.Function;
import uk.gov.companieshouse.service.rest.err.Err;

/**
 * Where in the HTTP request an invalid field is located. Selects the matching library {@link Err} builder.
 */
public enum ErrorLocation {
    BODY(Err::invalidBodyBuilderWithLocation),
    QUERY(Err::invalidQueryParameterBuilderWithLocation),
    PATH(Err::invalidPathBuilderWithLocation);

    private final Function<String, Err.ErrBuilder> builderFactory;

    ErrorLocation(final Function<String, Err.ErrBuilder> builderFactory) {
        this.builderFactory = builderFactory;
    }

    /**
     * @param field the name of the invalid field
     * @return a new error builder located at this kind of location
     */
    public Err.ErrBuilder builderFor(final String field) {
        return builderFactory.apply(field);
    }
}
