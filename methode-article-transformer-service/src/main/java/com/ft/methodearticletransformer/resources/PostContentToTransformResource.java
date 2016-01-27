package com.ft.methodearticletransformer.resources;

import com.codahale.metrics.annotation.Timed;
import com.ft.api.jaxrs.errors.ClientError;
import com.ft.api.jaxrs.errors.ServerError;
import com.ft.api.util.transactionid.TransactionIdUtils;
import com.ft.content.model.Content;
import com.ft.methodearticletransformer.methode.MethodeContentNotEligibleForPublishException;
import com.ft.methodearticletransformer.methode.MethodeMarkedDeletedException;
import com.ft.methodearticletransformer.methode.MethodeMissingBodyException;
import com.ft.methodearticletransformer.methode.MethodeMissingFieldException;
import com.ft.methodearticletransformer.methode.NotWebChannelException;
import com.ft.methodearticletransformer.methode.ResourceNotFoundException;
import com.ft.methodearticletransformer.methode.SourceApiUnavailableException;
import com.ft.methodearticletransformer.model.EomFile;
import com.ft.methodearticletransformer.transformation.EomFileProcessorForContentStore;

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

    private final EomFileProcessorForContentStore eomFileProcessorForContentStore;

    public PostContentToTransformResource(EomFileProcessorForContentStore eomFileProcessorForContentStore) {

		this.eomFileProcessorForContentStore = eomFileProcessorForContentStore;
	}

	public enum ErrorMessage {
		METHODE_FILE_NOT_FOUND("Article cannot be found in Methode"),
		UUID_REQUIRED("No UUID was passed"),
		INVALID_UUID("The UUID passed was invalid"),
		METHODE_CONTENT_TYPE_NOT_SUPPORTED("Invalid request - resource not an article"),
		NOT_WEB_CHANNEL("This is not a web channel story"),
		METHODE_FIELD_MISSING("Required methode field [%s] is missing"),
		METHODE_API_UNAVAILABLE("Methode api was unavailable"),
		DOCUMENT_STORE_API_UNAVAILABLE("Document store API was unavailable");

		private final String text;

		ErrorMessage(String text) {
			this.text = text;
		}

		@Override
		public String toString() {
			return text;
		}
	}

	@POST
	@Timed
	@Path("/{uuidString}")
	@QueryParam("preview")
	@Produces(MediaType.APPLICATION_JSON + CHARSET_UTF_8)
	public final Content doTransform(@PathParam("uuidString") String uuidString, @QueryParam("preview") boolean preview, EomFile eomFile, @Context HttpHeaders httpHeaders) {

		String transactionId = TransactionIdUtils.getTransactionIdOrDie(httpHeaders, uuidString, "Publish request");
		if (uuidString == null) {
			throw ClientError.status(400).context(uuidString).reason(ErrorMessage.UUID_REQUIRED).exception();
		}
		UUID uuid;
		try {
			uuid = UUID.fromString(uuidString);
		} catch (IllegalArgumentException iae) {
			throw ClientError.status(400)
					.reason(ErrorMessage.INVALID_UUID)
					.exception(iae);
		}
		try {
			if(preview) {
				return eomFileProcessorForContentStore.processPreview(eomFile, transactionId);
			}
			return eomFileProcessorForContentStore.processPublication(eomFile, transactionId);

		}catch(SourceApiUnavailableException e){
			throw ServerError.status(503)
					.reason(ErrorMessage.METHODE_API_UNAVAILABLE)
					.exception(e);
		}catch(ResourceNotFoundException e){
			throw ClientError.status(404)
					.reason(ErrorMessage.METHODE_FILE_NOT_FOUND)
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
		}catch(MethodeMissingBodyException e){
			throw ClientError.status(418)
					.error(e.getMessage())
					.exception(e);
		}catch(MethodeContentNotEligibleForPublishException e){
			throw ClientError.status(404)
					.context(uuid)
					.error(e.getMessage())
					.exception(e);
		}
	}
}


