package uk.gov.companieshouse.presentersapi.data;

/**
 * Implemented by every generated BiMap so they can be reported on generically.
 */
public interface ReferenceBiMap {

    /** Number of entries in the key to values mapping. */
    int forwardEntryCount();

    /** Number of entries in the value to keys mapping. */
    int reverseEntryCount();
}
