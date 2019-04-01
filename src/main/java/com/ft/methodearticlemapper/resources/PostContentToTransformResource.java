package com.ft.methodearticlemapper.resources;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ft.api.jaxrs.errors.ClientError;
import com.ft.content.model.Content;
import com.ft.methodearticlemapper.configuration.PropertySource;
import com.ft.methodearticlemapper.exception.MethodeContentNotEligibleForPublishException;
import com.ft.methodearticlemapper.exception.MethodeMarkedDeletedException;
import com.ft.methodearticlemapper.exception.MethodeMissingBodyException;
import com.ft.methodearticlemapper.exception.MethodeMissingFieldException;
import com.ft.methodearticlemapper.exception.NotWebChannelException;
import com.ft.methodearticlemapper.exception.UnsupportedTransformationModeException;
import com.ft.methodearticlemapper.exception.UntransformableMethodeContentException;
import com.ft.methodearticlemapper.model.EomFile;
import com.ft.methodearticlemapper.transformation.EomFileProcessor;
import com.ft.methodearticlemapper.transformation.TransformationException;
import com.ft.methodearticlemapper.transformation.TransformationMode;
import com.google.common.base.Strings;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.function.Supplier;
import java.util.UUID;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@Path("/")
public class PostContentToTransformResource {
    private static final Logger LOG = LoggerFactory.getLogger(PostContentToTransformResource.class);

    private static final String CHARSET_UTF_8 = ";charset=utf-8";
    private static final String TX_ID = "transaction_id";

    private static final Supplier<String> TX_ID_SUPPLIER = () -> {
        String txId = MDC.get(TX_ID);
        if (Strings.isNullOrEmpty(txId)) {
            throw new NullPointerException();
        }

        txId = txId.substring(txId.indexOf('=') + 1);

        return txId;
    };

    private final EomFileProcessor eomFileProcessor;
    private final PropertySource lastModifiedSource;
    private final PropertySource txIdSource;
    private final String txIdField;

    public PostContentToTransformResource(EomFileProcessor eomFileProcessor,
            PropertySource lastModifiedSource,
            PropertySource txIdSource, String txIdField) {
        this.eomFileProcessor = eomFileProcessor;
        this.lastModifiedSource = lastModifiedSource;
        this.txIdSource = txIdSource;
        this.txIdField = txIdField;
    }

	@POST
	@Timed
	@Path("/map")
	@Produces(MediaType.APPLICATION_JSON + CHARSET_UTF_8)
	@JsonIgnoreProperties(value = { "brands" })
	public final Content map(EomFile eomFile, @QueryParam("preview") boolean preview, @QueryParam("mode") String mode) {
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

		String txId = getTransactionId(eomFile);
		Date lastModified = getLastModifiedDate(eomFile);
		return processRequest(uuid, transformationMode, eomFile, txId, lastModified);
	}

	private Date getLastModifiedDate(EomFile eomFile) {
	    Date lastModified = null;
	    switch (lastModifiedSource) {
	        case fromNative:
	            String s = eomFile.getAdditionalProperties().get("lastModified");
	            if (!Strings.isNullOrEmpty(s)) {
	                try {
	                    lastModified = Date.from(Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(s)));
	                } catch (DateTimeException e) {
	                    LOG.warn("Invalid value for lastModified: uuid={} lastModified={}", eomFile.getUuid(), s);
	                }
	            }
	            break;

	        default:
	            lastModified = new Date();
	    }

	    return lastModified;
	}

    private String getTransactionId(EomFile eomFile) {
        String txId;
        switch (txIdSource) {
            case fromNative:
                txId = eomFile.getAdditionalProperties().get(txIdField);
                break;

            default:
                txId = TX_ID_SUPPLIER.get();
        }

        return txId;
    }

    private Content processRequest(String uuid, TransformationMode mode, EomFile eomFile, String transactionId, Date lastModified) {
        try {
            return eomFileProcessor.process(eomFile, mode, transactionId, lastModified);

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
        } catch (TransformationException e) {
            throw ClientError.status(SC_UNPROCESSABLE_ENTITY)
                    .context(uuid)
                    .error(e.getMessage())
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
            throw ClientError.status(SC_NOT_FOUND)
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


