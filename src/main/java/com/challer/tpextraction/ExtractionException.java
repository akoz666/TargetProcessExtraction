package com.challer.tpextraction;

/**
 * Is used for main exception thrown during the extraction process
 *
 * @author Cyril Haller - cyril.haller@gmail.com
 */
public class ExtractionException extends Exception {

    public ExtractionException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ExtractionException(final String message) {
        super(message);
    }

}
