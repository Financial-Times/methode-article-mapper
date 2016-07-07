package com.ft.methodearticletransformer.resources;

import com.codahale.metrics.annotation.Timed;
import com.ft.api.jaxrs.errors.ClientError;
import com.ft.api.jaxrs.errors.ServerError;
import com.ft.api.util.transactionid.TransactionIdUtils;
import com.ft.content.model.Content;
import com.ft.methodearticletransformer.methode.ContentSourceService;
import com.ft.methodearticletransformer.methode.DocumentStoreApiUnavailableException;
import com.ft.methodearticletransformer.methode.MethodeContentNotEligibleForPublishException;
import com.ft.methodearticletransformer.methode.MethodeMarkedDeletedException;
import com.ft.methodearticletransformer.methode.MethodeMissingBodyException;
import com.ft.methodearticletransformer.methode.MethodeMissingFieldException;
import com.ft.methodearticletransformer.methode.NotWebChannelException;
import com.ft.methodearticletransformer.methode.ResourceNotFoundException;
import com.ft.methodearticletransformer.methode.SourceApiUnavailableException;
import com.ft.methodearticletransformer.methode.UntransformableMethodeContentException;
import com.ft.methodearticletransformer.model.EomFile;
import com.ft.methodearticletransformer.transformation.EomFileProcessor;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Path("/content")
public class GetTransformedContentResource {

    private static final String CHARSET_UTF_8 = ";charset=utf-8";
    
    private final ContentSourceService contentSourceService;
    private final EomFileProcessor eomFileProcessor;

    public GetTransformedContentResource(ContentSourceService contentSourceService, EomFileProcessor eomFileProcessor) {
		this.contentSourceService = contentSourceService;
		this.eomFileProcessor = eomFileProcessor;
	}

	@GET
    @Timed
    @Path("/{uuidString}")
    @Produces(MediaType.APPLICATION_JSON + CHARSET_UTF_8)
    public final Content getByUuid(@PathParam("uuidString") String uuidString, @Context HttpHeaders httpHeaders) {
		
		String transactionId = TransactionIdUtils.getTransactionIdOrDie(httpHeaders, uuidString, "Publish request");
        if (uuidString == null) {
            throw ClientError.status(400).reason(ErrorMessage.UUID_REQUIRED).exception();
        }

		UUID uuid;
		try {
			uuid = UUID.fromString(uuidString);
		} catch (IllegalArgumentException iae) {
			throw ClientError.status(400)
					.reason(ErrorMessage.INVALID_UUID)
					.exception(iae);
		}

        Date lastModifiedDate = null;
        try {
        	EomFile eomFile = contentSourceService.fileByUuid(uuid, transactionId);
            lastModifiedDate = eomFile.getLastModified();
    		return eomFileProcessor.processPublication(eomFile, transactionId);

        } catch (SourceApiUnavailableException e) {
			throw ServerError.status(503)
                    .reason(ErrorMessage.METHODE_API_UNAVAILABLE)
                    .exception(e);
        } catch (ResourceNotFoundException e) {
			throw ClientError.status(404)
			.reason(ErrorMessage.METHODE_FILE_NOT_FOUND)
			.exception(e);
        } catch (MethodeMarkedDeletedException e) {
            throw ClientError.status(404)
            .context(buildContext(uuid, lastModifiedDateOrNow(lastModifiedDate)))
			.reason(ErrorMessage.METHODE_FILE_NOT_FOUND)
			.exception(e);
		} catch (NotWebChannelException e) {
			throw ClientError.status(404)
			.reason(ErrorMessage.NOT_WEB_CHANNEL)
			.exception(e);
		} catch (MethodeMissingFieldException e) {
			throw ClientError.status(404)
					.error(String.format(ErrorMessage.METHODE_FIELD_MISSING.toString(), e.getFieldName()))
					.exception(e);
		} catch (MethodeMissingBodyException | UntransformableMethodeContentException e) {
		    throw ClientError.status(418)
		            .error(e.getMessage())
		            .exception(e);
		} catch (MethodeContentNotEligibleForPublishException e) {
			throw ClientError.status(404)
			.context(buildContext(uuid, lastModifiedDateOrNow(lastModifiedDate)))
			.error(e.getMessage())
			.exception(e);
        } catch (DocumentStoreApiUnavailableException e) {
            throw ServerError.status(503)
                  .reason(ErrorMessage.DOCUMENT_STORE_API_UNAVAILABLE)
                  .exception(e);
        }
		
    }

    private Map<String, Object> buildContext(UUID uuid, Date messageTimestamp) {
        OffsetDateTime lastModified = OffsetDateTime.of(
            LocalDateTime.ofInstant(messageTimestamp.toInstant(), ZoneId.of(ZoneOffset.UTC.getId())),
            ZoneOffset.UTC
        );
        Map<String, Object> context = new HashMap<>();
        context.put("uuid", uuid);
        context.put("lastModified", lastModified);
        return context;
    }

	private Date lastModifiedDateOrNow(Date lastModifiedDate) {
		if (lastModifiedDate == null) {
			return new Date();
		} else {
			return lastModifiedDate;
		}
	}
}