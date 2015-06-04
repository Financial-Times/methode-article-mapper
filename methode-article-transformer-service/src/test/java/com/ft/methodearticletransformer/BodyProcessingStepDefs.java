package com.ft.methodearticletransformer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.xml.namespace.QName;

import com.ft.bodyprocessing.richcontent.ConvertParameters;
import com.ft.bodyprocessing.richcontent.VideoMatcher;
import com.ft.bodyprocessing.richcontent.VideoSiteConfiguration;
import com.ft.bodyprocessing.xml.eventhandlers.TransformingEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandler;
import com.ft.jerseyhttpwrapper.ResilientClient;
import com.ft.methodearticletransformer.transformation.BodyProcessingFieldTransformerFactory;
import com.ft.methodearticletransformer.transformation.FieldTransformer;
import com.ft.methodearticletransformer.transformation.MethodeBodyTransformationXMLEventHandlerRegistry;
import com.google.common.collect.ImmutableList;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.header.InBoundHeaders;
import com.sun.jersey.spi.MessageBodyWorkers;

import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

import org.apache.commons.lang.RandomStringUtils;
import org.codehaus.stax2.ri.evt.EntityReferenceEventImpl;
import org.codehaus.stax2.ri.evt.StartElementEventImpl;
import org.custommonkey.xmlunit.Diff;

public class BodyProcessingStepDefs {

    private String methodeBodyText;
    private String transformedBodyText;

    private FieldTransformer bodyTransformer;

    private static final String TEXT = "Some text in between tags";

    private ResilientClient semanticStoreContentReaderClient;
    private VideoMatcher videoMatcher;
    private URI uri;

    private InBoundHeaders headers;
    private MessageBodyWorkers workers;

    private InputStream entity;

    private MethodeBodyTransformationXMLEventHandlerRegistry registry;

    private static final String TRANSACTION_ID = randomChars(10);
    private Map<String, String> rulesAndHandlers;

    private static final List<String> T = Collections.singletonList("t");
    private static final List<String> NONE = Collections.emptyList();

    private static final String CONVERT_FROM_PARAMETER = "start";
    private static final String CONVERTED_TO_PARAMETER = "t";
    private static final String CONVERSION_TEMPLATE = "%ss";
    private static final ConvertParameters CONVERT_PARAMETERS = new ConvertParameters(CONVERT_FROM_PARAMETER, CONVERTED_TO_PARAMETER, CONVERSION_TEMPLATE);
    private static final List<ConvertParameters> CONVERT_PARAMETERS_LIST = ImmutableList.of(CONVERT_PARAMETERS);

    public static List<VideoSiteConfiguration> DEFAULTS = Arrays.asList(
            new VideoSiteConfiguration("https?://www.youtube.com/watch\\?v=(?<id>[A-Za-z0-9_-]+)", "https://www.youtube.com/watch?v=%s", true, T, null, true),
            new VideoSiteConfiguration("https?://www.youtube.com/embed/(?<id>[A-Za-z0-9_-]+)", "https://www.youtube.com/watch?v=%s", false, NONE, CONVERT_PARAMETERS_LIST, true),
            new VideoSiteConfiguration("https?://youtu.be/(?<id>[A-Za-z0-9_-]+)", "https://www.youtube.com/watch?v=%s", false, T, null, true),
            new VideoSiteConfiguration("https?://www.vimeo.com/(?<id>[0-9]+)", null, false, NONE, null, true),
            new VideoSiteConfiguration("//player.vimeo.com/video/(?<id>[0-9]+)", "https://www.vimeo.com/%s", true, NONE, null, true),
            new VideoSiteConfiguration("https?://video.ft.com/(?<id>[0-9]+)/", null, false, NONE, null, true)
    );

    private static String randomChars(int howMany) {
        return RandomStringUtils.randomAlphanumeric(howMany).toLowerCase();
    }

    @Before
    public void setup() throws Exception {
        semanticStoreContentReaderClient = mock(ResilientClient.class);

        uri = new URI("www.anyuri.com");
        videoMatcher = new VideoMatcher(DEFAULTS);
        headers = mock(InBoundHeaders.class);
        workers = mock(MessageBodyWorkers.class);
        entity = new ByteArrayInputStream("Test".getBytes(StandardCharsets.UTF_8));
        headers = mock(InBoundHeaders.class);
        workers = mock(MessageBodyWorkers.class);
        entity = new ByteArrayInputStream("Test".getBytes(StandardCharsets.UTF_8));
        bodyTransformer = new BodyProcessingFieldTransformerFactory(semanticStoreContentReaderClient, uri, videoMatcher).newInstance();
        registry = new MethodeBodyTransformationXMLEventHandlerRegistry(videoMatcher);

        rulesAndHandlers = new HashMap<>();
        rulesAndHandlers.put( "STRIP ELEMENT AND CONTENTS" , "StripElementAndContentsXMLEventHandler");
        rulesAndHandlers.put( "STRIP ELEMENT AND LEAVE CONTENT", "StripXMLEventHandler");
        rulesAndHandlers.put( "RETAIN ELEMENT AND REMOVE ATTRIBUTES", "RetainWithoutAttributesXMLEventHandler");
        rulesAndHandlers.put( "TRANSFORM THE TAG", "SimpleTransformTagXmlEventHandler");
        rulesAndHandlers.put( "CONVERT HTML ENTITY TO UNICODE", "PlainTextHtmlEntityReferenceEventHandler");
        rulesAndHandlers.put( "STRIP ELEMENT AND LEAVE CONTENT BY DEFAULT", "StripXMLEventHandler");
        rulesAndHandlers.put( "TRANSFORM THE WEB-PULL-QUOTE TO PULL-QUOTE", "PullQuoteEventHandler");
        rulesAndHandlers.put( "TRANSFORM TAG IF BIG NUMBER", "PromoBoxEventHandler");
		rulesAndHandlers.put( "TRANSFORM TAG IF PROMO BOX", "PromoBoxEventHandler");
		rulesAndHandlers.put( "TRANSFORM TAG IF PROMO BOX WITH MASTER IMAGE", "PromoBoxEventHandler");
        rulesAndHandlers.put( "RETAIN ELEMENT AND REMOVE FORMATTING ATTRIBUTES", "DataTableXMLEventHandler");
        rulesAndHandlers.put( "TRANSFORM THE SCRIPT ELEMENT TO PODCAST", "PodcastXMLEventHandler");
        rulesAndHandlers.put( "TRANSFORM THE TAG TO VIDEO", "MethodeBrightcoveVideoXmlEventHandler");
        rulesAndHandlers.put( "TRANSFORM OTHER VIDEO TYPES", "MethodeOtherVideoXmlEventHandler");
        rulesAndHandlers.put( "WRAP AND TRANSFORM A INLINE IMAGE", "WrappedHandlerXmlEventHandler");
        rulesAndHandlers.put( "REPLACE BLOCK ELEMENT TAG", "SimpleTransformBlockElementEventHandler");


        WebResource webResourceNotFound = mock(WebResource.class);
        when(semanticStoreContentReaderClient.resource(any(URI.class))).thenReturn(webResourceNotFound);
        WebResource.Builder builderNotFound = mock(WebResource.Builder.class);
        when(webResourceNotFound.accept(any(MediaType[].class))).thenReturn(builderNotFound);
        when(builderNotFound.header(anyString(), anyObject())).thenReturn(builderNotFound);
        when(builderNotFound.get(ClientResponse.class)).thenReturn(clientResponseWithCode(404));
        
        WebResource webResource = mock(WebResource.class);
        when(semanticStoreContentReaderClient.resource(UriBuilder.fromUri(uri).path("fbbee07f-5054-4a42-b596-64e0625d19a6").build())).thenReturn(webResource);
        WebResource.Builder builder = mock(WebResource.Builder.class);
        when(webResource.accept(MediaType.APPLICATION_JSON_TYPE)).thenReturn(builder);
        when(builder.header(anyString(), anyString())).thenReturn(builder);
        ClientResponse clientResponseSuccess = mock(ClientResponse.class);
        when(builder.get(ClientResponse.class)).thenReturn(clientResponseSuccess);
        InputStream inputStream = mock(InputStream.class);
        when(clientResponseSuccess.getEntityInputStream()).thenReturn(inputStream);
        when(clientResponseSuccess.getStatus()).thenReturn(200);

        bodyTransformer = new BodyProcessingFieldTransformerFactory(semanticStoreContentReaderClient, uri, videoMatcher).newInstance();
    }


    @Given("^the Methode body contains (.+) the transformer will (.+) and the replacement tag will be (.+)$")
    public void the_methode_body_contains_transforms_into(String tagname, String rule, String replacement) throws Throwable {
        assertTagIsRegisteredToTransform(rule, tagname, replacement);
    }

    @Given("^the Methode body has (.+) the transformer will (.+)$")
    public void the_methode_body_contains(String tagname, String rule) throws Throwable {
        assertTagIsRegistered(tagname,rule);
    }

    @Given("^I have a body text in Methode XML format containing (.+)$")
    public void i_have_body_text_in_methode_xml_format_containing(String tagname) throws Throwable {
        methodeBodyText = "<" + tagname + " title=\"title\">Text</" + tagname + ">";
    }

    @Given("^I have a body (.+?)$")
    public void I_have_a_body(String html) throws Throwable {
        methodeBodyText = "<body>" + html + "</body>";
    }

    @Given("^there are empty paragraphs in the body$")
    public void there_are_empty_paragraphs() throws Throwable {
        // no op!
    }

    @When("^I transform it into our Content Store format$")
    public void i_transform_it_into_our_content_store_format() throws Throwable {
        transformedBodyText = bodyTransformer.transform(methodeBodyText, TRANSACTION_ID);
    }

    @When("^I transform it$")
    public void I_transform_it() throws Throwable {
        transformedBodyText = bodyTransformer.transform(methodeBodyText, TRANSACTION_ID);
    }
    @Then("^it is left unmodified$")
    public void it_is_left_unmodified() {
        assertThat(transformedBodyText,equalToIgnoringCase(methodeBodyText));
    }

    @Then("^the start tag (.+) should have been removed$")
    public void the_start_tag_should_have_been_removed(String tagname) throws Throwable {
        assertThat("start tag wasn't removed", transformedBodyText, not(containsString("<" + tagname + ">")));
    }

    @Then("^the end tag (.+) should have been removed$")
    public void the_end_tag_should_have_been_removed(String tagname) throws Throwable {
        assertThat("end tag wasn't removed", transformedBodyText, not(containsString("</" + tagname + ">")));
    }

    @Then("^the text inside should not have been removed$")
    public void the_text_inside_should_not_have_been_removed() throws Throwable {
        assertThat("Text was removed", transformedBodyText, containsString("Text"));
    }

    @And("^the text inside should have been removed$")
    public void the_text_inside_should_have_been_removed() throws Throwable {
        assertThat("Text was removed", transformedBodyText, not(containsString("Text")));
    }

    @Then("^the start tag (.+) should have been replaced by (.+)$")
    public void the_start_tag_tagname_should_have_been_replaced_by_replacement(String tagname, String replacement) throws Throwable {
        assertThat("start tag was removed", transformedBodyText, containsString("<" + replacement + ">"));
    }

    @And("^the end tag (.+) should have been replaced by (.+)$")
    public void the_end_tag_tagname_should_have_been_replaced_by_replacement(String tagname, String replacement) throws Throwable {
        assertThat("end tag was removed", transformedBodyText, containsString("</" + replacement + ">"));
    }

    @Then("^the start tag (.+) should be present$")
    public void the_start_tag_tagname_should_be_present(String tagname) throws Throwable {
        assertThat("start tag was removed", transformedBodyText, containsString("<" + tagname + ">"));
    }

    @And("^the end tag (.+) should be present$")
    public void the_end_tag_tagname_should_be_present(String tagname) throws Throwable {
        assertThat("end tag was removed", transformedBodyText, containsString("</" + tagname + ">"));
    }

    @Given("^I have a? \".*\" in a Methode XML body like (.*)$")
    public void I_have_body_text_in_Methode_XML_like_before(String body) throws Throwable {
        methodeBodyText = body;
    }

    @Given("^I have a rule to (.+) and an entity (.+)$")
    public void I_have_a_rule_and_an_entity(String rule, String entity) throws Throwable {
        String handler = rulesAndHandlers.get(rule);
        String entitybasic = entity.substring(1, entity.length()-1);
        EntityReferenceEventImpl event = new EntityReferenceEventImpl(null, entitybasic);
        XMLEventHandler eventHandler = registry.getEventHandler(event);
        assertThat("The handler is incorrect", eventHandler.getClass().getSimpleName(), equalTo(handler));
    }

    @Given("^the before tag (.+) and the after tag (.+) adheres to the (.+) rule$")
    public void before_and_after_tag_name_adheres_to_rule(String name, String aftername, String rule) throws Throwable {
        assertTagIsRegisteredToTransform(rule, name, aftername);
    }

    @Then("^the body should be like (.*)$")
    public void the_body_should_be_like_after(String after) throws Throwable {
        assertThat("the body was not transformed as expected", transformedBodyText, is(after));
    }

    @Given("^an entity reference (.+)$")
    public void An_entity_reference_entity(String entity) throws Throwable {
        methodeBodyText = "<body>" + entity + "</body>";
    }

    @Then("^the entity should be replace by unicode codepoint (.+)$")
    public void the_entity_should_be_replace_by_unicode_codepoint_codepoint(String codepoint) throws Throwable {
        final int codePointInt = Integer.decode(codepoint);
        final char[] chars = Character.toChars(codePointInt);
        final String expected = "<body>" + new String(chars) + "</body>";
        assertThat(transformedBodyText, is(expected));
    }

    @Then("^it is transformed the entity (.+) should be replaced by the unicode codepoint (.+)$")
    public void the_entity_should_be_replace_by_unicode_codepoint(String entity, String codepoint) throws Throwable {
        int codePointInt = Integer.decode(codepoint);
        char[] chars = Character.toChars(codePointInt);
        String expected = "<body>" + TEXT  + new String(chars) + "</body>";
        methodeBodyText = "<body>" + TEXT  +  entity + "</body>";
        transformedBodyText = bodyTransformer.transform(methodeBodyText, TRANSACTION_ID);
        assertThat(transformedBodyText, is(expected));
    }

    @Given("^I have an? \".*\" in a Methode article body like (.*)$")
    public void I_have_something_in_a_Methode_article_body_like(String body) throws Throwable {
        methodeBodyText = body;
    }

    @Given("^the tag (.+) adheres to the (.+)$")
    public void tag_name_adheres_to_rule(String name, String rule) throws Throwable {
        assertTagIsRegistered(name, rule);
    }

    @When("^it is transformed, (.+) becomes (.+)$")
    public void the_before_becomes_after(String before, String after) throws Throwable {
        transformedBodyText = bodyTransformer.transform(wrapped(before), TRANSACTION_ID);
        assertThat("before and after do not match", transformedBodyText, equalTo(wrapped(after)));
    }

    private String wrapped(String bodyMarkUp) {
        return String.format("<body>%s</body>", bodyMarkUp);
    }

    @Then("^the hyperlink should be like (.*)$")
    public void the_hyperlink_should_be_like_after(String after) throws Throwable {
        Diff difference = new Diff(transformedBodyText, after);
        assertThat(String.format("the hyperlink was not transformed as expected, it was: [%s]", difference.toString()), difference.identical());
    }

    private void assertTagIsRegisteredToTransform(String rule, String before, String after){
        XMLEventHandler eventHandler = null;

        eventHandler = assertTagIsRegistered(before, rule);

        if(eventHandler instanceof TransformingEventHandler) {
            assertThat("The replacement tag is not registered properly", ((TransformingEventHandler)eventHandler).getNewElement(), equalTo(after));
        }
        else{
            assertThat("The transformer is not of type TransformableEvent", false);
        }

    }

    private XMLEventHandler assertTagIsRegistered( String name, String rule ) {
        String handler = rulesAndHandlers.get(rule);
        StartElementEventImpl startElement = StartElementEventImpl.construct(null, new QName(name), null, null, null);
        XMLEventHandler eventHandler = registry.getEventHandler(startElement);
        assertThat("handler incorrect", eventHandler.getClass().getSimpleName(), equalTo(handler));
        return eventHandler;
    }

    private ClientResponse clientResponseWithCode(int status) {
        return new ClientResponse(status, headers, entity, workers);
    }

}