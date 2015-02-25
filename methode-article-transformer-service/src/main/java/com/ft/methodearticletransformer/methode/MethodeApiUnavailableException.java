package com.ft.methodearticletransformer.methode;

/**
 * MethodeApiUnavailableException
 *
 * @author Simon.Gibbs
 */
public class MethodeApiUnavailableException extends RuntimeException {
	private static final long serialVersionUID = 8803441501377423575L;

	public MethodeApiUnavailableException(Throwable cause) {
        super(cause.getMessage(),cause);
    }

    public MethodeApiUnavailableException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
