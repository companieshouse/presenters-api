package uk.gov.companieshouse.presentersapi.guard;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import uk.gov.companieshouse.service.ServiceResult;
import uk.gov.companieshouse.service.rest.err.Err;
import uk.gov.companieshouse.service.rest.err.Errors;

/**
 * The outcome of guarding one input: either a value, or the non-empty list of {@link Err}s explaining why it is
 * not valid. Immutable. Guard rules return this instead of throwing, so services can run every check, collect
 * all the problems with {@link #combinedErrors(Guarded[])} and return one {@link ServiceResult}.
 *
 * @param <T> the type of the guarded value
 */
public final class Guarded<T> {

    private final T value;
    private final List<Err> errors;

    private Guarded(final T value, final List<Err> errors) {
        this.value = value;
        this.errors = errors;
    }

    /**
     * @param value the guarded, non-null value
     * @param <T>   the type of the value
     * @return a valid result holding the value
     */
    public static <T> Guarded<T> valid(final T value) {
        return new Guarded<>(Objects.requireNonNull(value), List.of());
    }

    /**
     * @param error the non-null reason the input is not valid
     * @param <T>   the type of the value that was expected
     * @return an invalid result holding the error
     */
    public static <T> Guarded<T> invalid(final Err error) {
        return new Guarded<>(null, List.of(Objects.requireNonNull(error)));
    }

    /**
     * @param errors the non-empty reasons the input is not valid
     * @param <T>    the type of the value that was expected
     * @return an invalid result holding a copy of the errors
     * @throws IllegalArgumentException if errors is empty
     */
    public static <T> Guarded<T> invalid(final List<Err> errors) {
        if (errors.isEmpty()) {
            throw new IllegalArgumentException("An invalid result needs at least one error");
        }
        return new Guarded<>(null, List.copyOf(errors));
    }

    /**
     * @param results the results to combine, in order
     * @return all the errors of the invalid results, in order; has no errors when every result is valid
     */
    public static Errors combinedErrors(final Guarded<?>... results) {
        final var combined = new Errors();
        for (final var result : results) {
            result.errors.forEach(combined::addError);
        }
        return combined;
    }

    /**
     * @return true when the input was valid
     */
    public boolean isValid() {
        return errors.isEmpty();
    }

    /**
     * @return the guarded value
     * @throws IllegalStateException if this result is not valid
     */
    public T value() {
        if (!isValid()) {
            throw new IllegalStateException("No value: guard failed with " + errors);
        }
        return value;
    }

    /**
     * @return an unmodifiable list of errors, empty when valid
     */
    public List<Err> errors() {
        return errors;
    }

    /**
     * @param mapper converts a valid value; not called when invalid
     * @param <R>    the type of the converted value
     * @return a valid result holding the converted value, or this result's errors unchanged
     */
    public <R> Guarded<R> map(final Function<? super T, ? extends R> mapper) {
        return isValid() ? valid(mapper.apply(value)) : new Guarded<>(null, errors);
    }

    /**
     * @param mapper applies a further rule to a valid value; not called when invalid
     * @param <R>    the type of the further result
     * @return the result of the further rule, or this result's errors unchanged
     */
    public <R> Guarded<R> flatMap(final Function<? super T, Guarded<R>> mapper) {
        return isValid() ? mapper.apply(value) : new Guarded<>(null, errors);
    }

    /**
     * @return a found result holding the value when valid, otherwise an invalid result holding all the errors
     */
    public ServiceResult<T> toServiceResult() {
        if (isValid()) {
            return ServiceResult.found(value);
        }
        return ServiceResult.invalid(combinedErrors(this));
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof final Guarded<?> that && Objects.equals(value, that.value) && errors.equals(that.errors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, new ArrayList<>(errors));
    }

    @Override
    public String toString() {
        return isValid() ? "Valid[%s]".formatted(value) : "Invalid%s".formatted(errors);
    }
}
