package com.ft.methodearticletransformer.methode;

public class SemanticReaderUnavailableException extends RuntimeException {


    private static final long serialVersionUID = 52256633187754667L;
    
    
    /** Constructor for cases where there is no known underlying cause of this exception.
     *  @param message a message
     */
    public SemanticReaderUnavailableException(String message) {
        super(message);
    }
    
    public SemanticReaderUnavailableException(Throwable throwable) {
        super(throwable);
    }

}
