package com.ft.methodearticletransformer.methode;

/**
 * SourceApiUnavailableException
 *
 * @author Simon.Gibbs
 */
public class SourceApiUnavailableException extends RuntimeException {
	private static final long serialVersionUID = 8803441501377423575L;

	public SourceApiUnavailableException(Throwable cause) {
        super(cause.getMessage(),cause);
    }

    public SourceApiUnavailableException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
