package com.ft.methodetransformer.methode;

import com.ft.api.jaxrs.client.exceptions.RemoteApiException;

/**
 * UnexpectedMethodeApiException
 *
 * @author Simon.Gibbs
 */
public class UnexpectedMethodeApiException extends RuntimeException {
	private static final long serialVersionUID = 2878959132253974219L;

	public UnexpectedMethodeApiException(RemoteApiException cause) {
        super(cause.getMessage(),cause);
    }

}
