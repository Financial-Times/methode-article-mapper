package com.ft.methodearticletransformer.methode;

import com.ft.api.jaxrs.client.exceptions.RemoteApiException;

/**
 * MethodeApiUnavailableException
 *
 * @author Simon.Gibbs
 */
public class MethodeApiUnavailableException extends RuntimeException {
	private static final long serialVersionUID = 8803441501377423575L;

	public MethodeApiUnavailableException(RemoteApiException cause) {
        super(cause.getMessage(),cause);
    }

}
