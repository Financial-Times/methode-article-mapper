package com.ft.methodearticlemapper.methode;

public class DocumentStoreApiUnavailableException extends RuntimeException {


    private static final long serialVersionUID = 52256633187754667L;
    
    
    /** Constructor for cases where there is no known underlying cause of this exception.
     *  @param message a message
     */
    public DocumentStoreApiUnavailableException(String message) {
        super(message);
    }
    
    public DocumentStoreApiUnavailableException(Throwable throwable) {
        super(throwable);
    }

}
