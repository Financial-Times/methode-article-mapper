package com.ft.methodearticlemapper.methode;

import com.ft.api.jaxrs.client.exceptions.RemoteApiException;

/**
 * UnexpectedSourceApiException
 *
 * @author Simon.Gibbs
 */
public class UnexpectedSourceApiException extends RuntimeException {
	private static final long serialVersionUID = 2878959132253974219L;

	public UnexpectedSourceApiException(RemoteApiException cause) {
        super(cause.getMessage(),cause);
    }

}
