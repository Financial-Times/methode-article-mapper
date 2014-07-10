package com.ft.methodetransformer.resources;


import java.util.Date;
import java.util.UUID;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.ft.contentstoreapi.model.Content;
import com.yammer.metrics.annotation.Timed;

@Path("/content")
public class MethodeTransformerResource {

    private static final String CHARSET_UTF_8 = ";charset=utf-8";

    @GET
    @Timed
    @Path("/{uuid}")
    @Produces(MediaType.APPLICATION_JSON + CHARSET_UTF_8)
    public final Content getByUuid(@PathParam("uuid") String uuid) {

        return Content.builder()
                .withHeadline("a headline")
                .withByline("By someone")
                .withLastPublicationDate(new Date(300L))
                .withUuid(UUID.fromString(uuid))
                .withXmlBody("The body")
                .build();

    }
}
