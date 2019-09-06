package org.gridgain.dr;

public class CacheContentsMismatchException extends Exception {
    public CacheContentsMismatchException(String message) {
        super(message);
    }

    public CacheContentsMismatchException(String message, CacheContentsMismatchException cause) {
        super(message, cause);
    }
}
