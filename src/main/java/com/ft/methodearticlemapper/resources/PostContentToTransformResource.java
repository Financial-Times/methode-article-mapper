package com.ft.methodearticlemapper.resources;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;

import com.codahale.metrics.annotation.Timed;
import com.ft.api.jaxrs.errors.ClientError;
import com.ft.api.util.transactionid.TransactionIdUtils;
import com.ft.content.model.Content;
import com.ft.methodearticlemapper.exception.MethodeContentNotEligibleForPublishException;
import com.ft.methodearticlemapper.exception.MethodeMarkedDeletedException;
import com.ft.methodearticlemapper.exception.MethodeMissingBodyException;
import com.ft.methodearticlemapper.exception.MethodeMissingFieldException;
import com.ft.methodearticlemapper.exception.NotWebChannelException;
import com.ft.methodearticlemapper.exception.UnsupportedTransformationModeException;
import com.ft.methodearticlemapper.exception.UntransformableMethodeContentException;
import com.ft.methodearticlemapper.model.EomFile;
import com.ft.methodearticlemapper.transformation.EomFileProcessor;
import com.ft.methodearticlemapper.transformation.TransformationMode;

import java.util.Date;
import java.util.UUID;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

@Path("/")
public class PostContentToTransformResource {

    private static final String CHARSET_UTF_8 = ";charset=utf-8";

    // strictly this is an HTTP/2 response code, so there's no constant for it
    private static final int SC_MISDIRECTED_REQUEST = 421;
    
    private final EomFileProcessor eomFileProcessor;

    public PostContentToTransformResource(EomFileProcessor eomFileProcessor) {

        this.eomFileProcessor = eomFileProcessor;
    }

	@POST
	@Timed
	@Path("/map")
	@Produces(MediaType.APPLICATION_JSON + CHARSET_UTF_8)
	public final Content map(EomFile eomFile, @QueryParam("preview") boolean preview, @QueryParam("mode") String mode, @Context HttpHeaders httpHeaders) {
		final String transactionId = TransactionIdUtils.getTransactionIdOrDie(httpHeaders);
		final String uuid = eomFile.getUuid();
		validateUuid(uuid);
		
		TransformationMode transformationMode = null;
		if (mode == null) {
		    transformationMode = preview ? TransformationMode.PREVIEW : TransformationMode.PUBLISH;
		} else {
		    try {
		        transformationMode = TransformationMode.valueOf(mode.toUpperCase());
		    } catch (IllegalArgumentException e) {
	            throw ClientError.status(SC_BAD_REQUEST)
                                 .context(uuid)
                                 .error(e.getMessage())
                                 .exception(e);
		    }
		}
        // otherwise, mode trumps the preview flag if there is a mismatch
		
		return processRequest(uuid, transformationMode, eomFile, transactionId);
	}

    private Content processRequest(String uuid, TransformationMode mode, EomFile eomFile, String transactionId) {
        try {
            return eomFileProcessor.process(eomFile, mode, transactionId, new Date());

        } catch (MethodeMarkedDeletedException e) {
            throw ClientError.status(SC_NOT_FOUND)
                    .context(uuid)
                    .reason(ErrorMessage.METHODE_FILE_NOT_FOUND)
                    .exception(e);
        } catch (NotWebChannelException e) {
            throw ClientError.status(SC_UNPROCESSABLE_ENTITY)
                    .reason(ErrorMessage.NOT_WEB_CHANNEL)
                    .exception(e);
        } catch (MethodeMissingFieldException e) {
            throw ClientError.status(SC_UNPROCESSABLE_ENTITY)
                    .error(String.format(ErrorMessage.METHODE_FIELD_MISSING.toString(), e.getFieldName()))
                    .exception(e);
        } catch (MethodeMissingBodyException | UntransformableMethodeContentException e) {
            throw ClientError.status(SC_UNPROCESSABLE_ENTITY)
                    .error(e.getMessage())
                    .exception(e);
        } catch (MethodeContentNotEligibleForPublishException e) {
            throw ClientError.status(SC_UNPROCESSABLE_ENTITY)
                    .context(uuid)
                    .error(e.getMessage())
                    .exception(e);
        } catch (UnsupportedTransformationModeException e) {
            throw ClientError.status(SC_MISDIRECTED_REQUEST)
                    .context(uuid)
                    .error(e.getMessage())
                    .exception(e);
        }
    }

    private void validateUuid(String uuid) {
        if (uuid == null) {
            throw ClientError.status(SC_BAD_REQUEST).context(null).reason(ErrorMessage.UUID_REQUIRED).exception();
        }
        try {
            if (!UUID.fromString(uuid).toString().equals(uuid)) {
                throw new IllegalArgumentException("Invalid UUID: " + uuid);
            }
        } catch (IllegalArgumentException iae) {
            throw ClientError.status(SC_BAD_REQUEST)
                    .reason(ErrorMessage.INVALID_UUID)
                    .exception(iae);
        }
    }

    enum ErrorMessage {
        UUID_REQUIRED("No UUID was passed"),
        INVALID_UUID("The UUID passed was invalid"),
        METHODE_FILE_NOT_FOUND("Article marked as deleted"),
        NOT_WEB_CHANNEL("This is not a web channel story"),
        METHODE_FIELD_MISSING("Required methode field [%s] is missing");

        private final String text;

        ErrorMessage(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }
}


