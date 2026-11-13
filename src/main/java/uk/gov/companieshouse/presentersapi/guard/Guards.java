package uk.gov.companieshouse.presentersapi.guard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import uk.gov.companieshouse.service.rest.err.Err;

/**
 * Reusable code-lookup rules for any enum that offers a non-throwing {@code find(String)}. None of them throw.
 */
public final class Guards {

    private Guards() {
    }

    /**
     * Resolves a single required code.
     *
     * @param code    the code from the request, may be null
     * @param find    the non-throwing lookup, which must accept null
     * @param missing the error when the code is null or blank
     * @param invalid the error when the code is unknown
     * @param <E>     the type the code resolves to
     * @return the resolved constant, or the missing or invalid error
     */
    public static <E> Guarded<E> requiredCode(
        final String code,
        final Function<String, Optional<? extends E>> find,
        final Supplier<Err> missing,
        final Supplier<Err> invalid) {
        if (code == null || code.isBlank()) {
            return Guarded.invalid(missing.get());
        }
        return find.apply(code)
                   .<Guarded<E>>map(Guarded::valid)
                   .orElseGet(() -> Guarded.invalid(invalid.get()));
    }

    /**
     * Resolves a list of codes. The checks run in order and the first failure wins: no values supplied, then any
     * null or unknown code, then duplicates. Duplicates are judged on the resolved constants, so an unknown code
     * is never reported as a duplicate.
     *
     * @param codes      the codes from the request, may be null
     * @param find       the non-throwing lookup, which must accept null
     * @param missing    the error when the list is null or empty
     * @param notValid   builds one error from comma-separated distinct invalid codes; null becomes "null"
     * @param duplicated builds one error from comma-separated duplicated constants' first supplied codes
     * @param <E>        the type the codes resolve to
     * @return the resolved constants or one error with rejected codes in first-occurrence order; invalid codes suppress duplicates
     */
    public static <E> Guarded<List<E>> codeList(
        final List<String> codes,
        final Function<String, Optional<? extends E>> find,
        final Supplier<Err> missing,
        final Function<String, Err> notValid,
        final Function<String, Err> duplicated) {
        if (codes == null || codes.isEmpty()) {
            return Guarded.invalid(missing.get());
        }

        final var resolved = new ArrayList<E>(codes.size());
        final var invalidCodes = new LinkedHashSet<String>();
        for (final var code : codes) {
            final var constant = find.apply(code);
            if (constant.isEmpty()) {
                invalidCodes.add(code);
            } else {
                resolved.add(constant.get());
            }
        }

        if (!invalidCodes.isEmpty()) {
            return Guarded.invalid(notValid.apply(
                String.join(", ", invalidCodes.stream().map(String::valueOf).toList())));
        }

        final var counts = new HashMap<E, Integer>();
        resolved.forEach(constant -> counts.merge(constant, 1, Integer::sum));
        final var reported = new HashSet<E>();
        final var duplicateCodes = new ArrayList<String>();
        for (var index = 0; index < resolved.size(); index++) {
            final var constant = resolved.get(index);
            if (counts.get(constant) > 1 && reported.add(constant)) {
                duplicateCodes.add(codes.get(index));
            }
        }
        if (!duplicateCodes.isEmpty()) {
            return Guarded.invalid(duplicated.apply(String.join(", ", duplicateCodes)));
        }
        return Guarded.valid(List.copyOf(resolved));
    }
}
