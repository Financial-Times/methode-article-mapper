package com.ft.methodearticlemapper.resources;

import com.codahale.metrics.annotation.Timed;
import com.ft.api.jaxrs.errors.ClientError;
import com.ft.api.jaxrs.errors.ServerError;
import com.ft.api.util.transactionid.TransactionIdUtils;
import com.ft.content.model.Content;
import com.ft.methodearticlemapper.methode.MethodeContentInvalidException;
import com.ft.methodearticlemapper.methode.MethodeContentNotEligibleForPublishException;
import com.ft.methodearticlemapper.methode.MethodeMarkedDeletedException;
import com.ft.methodearticlemapper.methode.MethodeMissingBodyException;
import com.ft.methodearticlemapper.methode.MethodeMissingFieldException;
import com.ft.methodearticlemapper.methode.NotWebChannelException;
import com.ft.methodearticlemapper.methode.SourceApiUnavailableException;
import com.ft.methodearticlemapper.methode.UntransformableMethodeContentException;
import com.ft.methodearticlemapper.model.EomFile;
import com.ft.methodearticlemapper.transformation.EomFileProcessor;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

@Path("/content-transform")
public class PostContentToTransformResource {

    private static final String CHARSET_UTF_8 = ";charset=utf-8";

    private final EomFileProcessor eomFileProcessor;

    public PostContentToTransformResource(EomFileProcessor eomFileProcessor) {

		this.eomFileProcessor = eomFileProcessor;
	}

	@POST
	@Timed
	@Path("/{uuidString}")
	@QueryParam("preview")
	@Produces(MediaType.APPLICATION_JSON + CHARSET_UTF_8)
	public final Content doTransform(@PathParam("uuidString") String uuid, @QueryParam("preview") boolean preview, EomFile eomFile, @Context HttpHeaders httpHeaders) {

		String transactionId = TransactionIdUtils.getTransactionIdOrDie(httpHeaders, uuid, "Publish request");

		validateUuid(uuid, eomFile);

		try {
			if(preview) {
				return eomFileProcessor.processPreview(eomFile, transactionId);
			}
			return eomFileProcessor.processPublication(eomFile, transactionId);

		}catch(SourceApiUnavailableException e){
			throw ServerError.status(503)
					.reason(ErrorMessage.METHODE_API_UNAVAILABLE)
					.exception(e);
		}catch(MethodeMarkedDeletedException e){
			throw ClientError.status(404)
					.context(uuid)
					.reason(ErrorMessage.METHODE_FILE_NOT_FOUND)
					.exception(e);
		}catch(NotWebChannelException e){
			throw ClientError.status(404)
					.reason(ErrorMessage.NOT_WEB_CHANNEL)
					.exception(e);
		}catch(MethodeMissingFieldException e){
			throw ClientError.status(404)
					.error(String.format(ErrorMessage.METHODE_FIELD_MISSING.toString(), e.getFieldName()))
					.exception(e);
		}catch (MethodeMissingBodyException | UntransformableMethodeContentException e) {
			throw ClientError.status(418)
					.error(e.getMessage())
					.exception(e);
		}catch(MethodeContentNotEligibleForPublishException e){
			throw ClientError.status(404)
					.context(uuid.toString())
					.error(e.getMessage())
					.exception(e);
		}
	}

	private void validateUuid(String uuid, EomFile eomFile) {
		if (uuid == null) {
			throw ClientError.status(400).context(uuid).reason(ErrorMessage.UUID_REQUIRED).exception();
		}
		try {

			UUID resourceId = UUID.fromString(uuid);

			if(!uuid.equals(eomFile.getUuid())) {
				String errorMessage = String.format(ErrorMessage.CONFLICTING_UUID.toString(), uuid, eomFile.getUuid());
				throw ClientError.status(409)
						.error(errorMessage)
						.exception(new MethodeContentInvalidException(resourceId, errorMessage));
			}
		}catch (IllegalArgumentException iae) {
			throw ClientError.status(400)
					.reason(ErrorMessage.INVALID_UUID)
					.exception(iae);
		}
	}
}


