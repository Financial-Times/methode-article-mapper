package com.ft.methodearticlemapper.resources;

import com.codahale.metrics.annotation.Timed;
import com.ft.api.jaxrs.errors.ClientError;
import com.ft.api.util.transactionid.TransactionIdUtils;
import com.ft.content.model.Content;
import com.ft.methodearticlemapper.exception.MethodeContentInvalidException;
import com.ft.methodearticlemapper.exception.MethodeContentNotEligibleForPublishException;
import com.ft.methodearticlemapper.exception.MethodeMarkedDeletedException;
import com.ft.methodearticlemapper.exception.MethodeMissingBodyException;
import com.ft.methodearticlemapper.exception.MethodeMissingFieldException;
import com.ft.methodearticlemapper.exception.NotWebChannelException;
import com.ft.methodearticlemapper.exception.UntransformableMethodeContentException;
import com.ft.methodearticlemapper.model.EomFile;
import com.ft.methodearticlemapper.transformation.EomFileProcessor;

import java.util.Date;
import java.util.UUID;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

@Path("/")
public class PostContentToTransformResource {

    private static final String CHARSET_UTF_8 = ";charset=utf-8";

    private final EomFileProcessor eomFileProcessor;

    public PostContentToTransformResource(EomFileProcessor eomFileProcessor) {

		this.eomFileProcessor = eomFileProcessor;
	}

	@POST
	@Timed
	@Path("/content-transform/{uuidString}")
	@QueryParam("preview")
	@Produces(MediaType.APPLICATION_JSON + CHARSET_UTF_8)
	public final Content doTransform(@PathParam("uuidString") String uuid, @QueryParam("preview") boolean preview, EomFile eomFile, @Context HttpHeaders httpHeaders) {
		String transactionId = TransactionIdUtils.getTransactionIdOrDie(httpHeaders);

		validateUuid(uuid, eomFile);

		return processRequest(uuid, preview, eomFile, transactionId);
	}

	@POST
	@Timed
	@Path("/map")
	@Produces(MediaType.APPLICATION_JSON + CHARSET_UTF_8)
	public final Content map(EomFile eomFile, @Context HttpHeaders httpHeaders) {
        return doTransform(eomFile.getUuid(), false, eomFile, httpHeaders);
	}

	private Content processRequest(String uuid, boolean preview, EomFile eomFile, String transactionId) {
		try {
			if(preview) {
				return eomFileProcessor.processPreview(eomFile, transactionId);
			}
			return eomFileProcessor.processPublication(eomFile, transactionId, new Date());

		}catch(MethodeMarkedDeletedException e){
			throw ClientError.status(404)
					.context(uuid)
					.reason(ErrorMessage.METHODE_FILE_NOT_FOUND)
					.exception(e);
		}catch(NotWebChannelException e){
			throw ClientError.status(422)
					.reason(ErrorMessage.NOT_WEB_CHANNEL)
					.exception(e);
		}catch(MethodeMissingFieldException e){
			throw ClientError.status(422)
					.error(String.format(ErrorMessage.METHODE_FIELD_MISSING.toString(), e.getFieldName()))
					.exception(e);
		}catch (MethodeMissingBodyException | UntransformableMethodeContentException e) {
			throw ClientError.status(422)
					.error(e.getMessage())
					.exception(e);
		}catch(MethodeContentNotEligibleForPublishException e){
			throw ClientError.status(422)
					.context(uuid)
					.error(e.getMessage())
					.exception(e);
		}
	}

	private void validateUuid(String uuid, EomFile eomFile) {
		if (uuid == null) {
			throw ClientError.status(400).context(null).reason(ErrorMessage.UUID_REQUIRED).exception();
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

	enum ErrorMessage {
		METHODE_FILE_NOT_FOUND("Article marked as deleted"),
		UUID_REQUIRED("No UUID was passed"),
		INVALID_UUID("The UUID passed was invalid"),
		METHODE_CONTENT_TYPE_NOT_SUPPORTED("Invalid request - resource not an article"),
		NOT_WEB_CHANNEL("This is not a web channel story"),
		METHODE_FIELD_MISSING("Required methode field [%s] is missing"),
		DOCUMENT_STORE_API_UNAVAILABLE("Document store API was unavailable"),
		CONFLICTING_UUID("UUID in the url [%s] and uuid in the payload [%s] are not the same");

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


