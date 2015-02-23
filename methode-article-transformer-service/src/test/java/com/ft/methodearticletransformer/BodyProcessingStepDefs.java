package com.ft.methodearticletransformer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.xml.namespace.QName;

import com.ft.bodyprocessing.xml.eventhandlers.SimpleTransformTagXmlEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandler;
import com.ft.methodearticletransformer.transformation.MethodeBodyTransformationXMLEventHandlerRegistry;
import org.apache.commons.lang.RandomStringUtils;
import org.codehaus.stax2.ri.evt.EntityReferenceEventImpl;
import org.codehaus.stax2.ri.evt.StartElementEventImpl;
import org.custommonkey.xmlunit.Diff;

import com.ft.jerseyhttpwrapper.ResilientClient;
import com.ft.methodeapi.model.EomAssetType;
import com.ft.methodearticletransformer.methode.MethodeFileService;
import com.ft.methodearticletransformer.transformation.BodyProcessingFieldTransformerFactory;
import com.ft.methodearticletransformer.transformation.FieldTransformer;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.header.InBoundHeaders;
import com.sun.jersey.spi.MessageBodyWorkers;

import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class BodyProcessingStepDefs {
	
	private String methodeBodyText;
	private String transformedBodyText;
	private FieldTransformer bodyTransformer;

    private static final String TRANSACTION_ID = randomChars(10);
    private static final String TEXT = "Some text in between tags";

    private MethodeFileService methodeFileService;
	private ResilientClient semanticStoreContentReaderClient;

	private InBoundHeaders headers;
	private MessageBodyWorkers workers;

	private InputStream entity;

    private MethodeBodyTransformationXMLEventHandlerRegistry registry;

    private Map<String, String> rulesAndHandlers;

    private static String randomChars(int howMany) {
        return RandomStringUtils.randomAlphanumeric(howMany).toLowerCase();
    }

	@Before
    public void setup() {
        methodeFileService = mock(MethodeFileService.class);
        semanticStoreContentReaderClient = mock(ResilientClient.class);
        headers = mock(InBoundHeaders.class);
        workers = mock(MessageBodyWorkers.class);
        entity = new ByteArrayInputStream("Test".getBytes(StandardCharsets.UTF_8));
        bodyTransformer = new BodyProcessingFieldTransformerFactory(methodeFileService, semanticStoreContentReaderClient).newInstance();
        registry = new MethodeBodyTransformationXMLEventHandlerRegistry();

        rulesAndHandlers = new HashMap<>();
        rulesAndHandlers.put( "STRIP ELEMENT AND CONTENTS" , "StripElementAndContentsXMLEventHandler");
        rulesAndHandlers.put( "STRIP ELEMENT AND LEAVE CONTENT", "StripXMLEventHandler");
        rulesAndHandlers.put( "RETAIN ELEMENT AND REMOVE ATTRIBUTES", "RetainWithoutAttributesXMLEventHandler");
        rulesAndHandlers.put( "TRANSFORM THE TAG", "SimpleTransformTagXmlEventHandler");
        rulesAndHandlers.put( "CONVERT HTML ENTITY TO UNICODE", "PlainTextHtmlEntityReferenceEventHandler");
        rulesAndHandlers.put( "STRIP ELEMENT AND LEAVE CONTENT BY DEFAULT", "StripXMLEventHandler");
        rulesAndHandlers.put( "TRANSFORM THE TAG TO PULL QUOTE", "PullQuoteEventHandler");
        rulesAndHandlers.put( "TRANSFORM TAG IF BIG NUMBER", "PromoBoxEventHandler");
        rulesAndHandlers.put( "TRANSFORM THE TAG TO TABLE", "DataTableXMLEventHandler");
        rulesAndHandlers.put( "TRANSFORM PODCAST ELEMENT", "PodcastXMLEventHandler");
        rulesAndHandlers.put( "TRANSFORM THE TAG TO VIDEO", "MethodeBrightcoveVideoXmlEventHandler");
        rulesAndHandlers.put( "TRANSFORM OTHER VIDEO TYPES", "MethodeOtherVideoXmlEventHandler");

        when(methodeFileService.assetTypes(anySet(), anyString())).thenReturn(Collections.<String, EomAssetType>emptyMap());

        EomAssetType compoundStoryAsset = new EomAssetType.Builder()
        	.uuid("fbbee07f-5054-4a42-b596-64e0625d19a6")
        	.type("EOM::CompoundStory")
        	.build();
		when(methodeFileService.assetTypes(Collections.singleton("fbbee07f-5054-4a42-b596-64e0625d19a6"), TRANSACTION_ID))
			.thenReturn(Collections.<String, EomAssetType>singletonMap("fbbee07f-5054-4a42-b596-64e0625d19a6", compoundStoryAsset));

		EomAssetType storyAsset = new EomAssetType.Builder()
    		.uuid("2d5f0ee9-09b3-4b09-af1b-e340276c7d6b")
    		.type("EOM::Story")
    		.build();
		when(methodeFileService.assetTypes(Collections.singleton("2d5f0ee9-09b3-4b09-af1b-e340276c7d6b"), TRANSACTION_ID))
			.thenReturn(Collections.<String, EomAssetType>singletonMap("2d5f0ee9-09b3-4b09-af1b-e340276c7d6b", storyAsset));


        EomAssetType pdfAsset = new EomAssetType.Builder()
                .uuid("5e231aca-a42b-11e1-a701-00144feabdc0")
                .type("Pdf")
                .build();
        when(methodeFileService.assetTypes(Collections.singleton("5e231aca-a42b-11e1-a701-00144feabdc0"), TRANSACTION_ID))
                .thenReturn(Collections.<String, EomAssetType>singletonMap("5e231aca-a42b-11e1-a701-00144feabdc0", pdfAsset));

        EomAssetType pageAsset = new EomAssetType.Builder()
        		.uuid("ee08dbdc-cd25-11de-a748-00144feabdc0")
        		.type("EOM::WebPage")
        		.build();
        when(methodeFileService.assetTypes(Collections.singleton("ee08dbdc-cd25-11de-a748-00144feabdc0"), TRANSACTION_ID))
        		.thenReturn(Collections.<String, EomAssetType>singletonMap("ee08dbdc-cd25-11de-a748-00144feabdc0", pageAsset));

		WebResource webResource = mock(WebResource.class);
		when(semanticStoreContentReaderClient.resource(any(URI.class))).thenReturn(webResource);
		WebResource.Builder builder = mock(WebResource.Builder.class);
		when(webResource.accept(any(MediaType[].class))).thenReturn(builder);
		when(builder.header(anyString(), anyObject())).thenReturn(builder);
		when(builder.get(ClientResponse.class)).thenReturn(clientResponseWithCode(404));


		bodyTransformer = new BodyProcessingFieldTransformerFactory(methodeFileService, semanticStoreContentReaderClient).newInstance();
    }

    @Given("^a replacement tag (.+) and the Methode body contains (.+) the transformer will (.+)$")
    public void the_methode_body_contains_transforms_into(String replacement, String tagname, String rule) throws Throwable {
        assertTagIsRegisteredToTransform(rule, tagname, replacement);
    }

    @Given("^the Methode body contains (.+) the transformer will (.+)$")
    public void the_methode_body_contains(String tagname, String rule) throws Throwable {
        assertTagIsRegistered(tagname,rule);
    }

    @Given("^I have body text in Methode XML format containing (.+)$")
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

    @Given("^I have body text in Methode XML like (.*)$")
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
        SimpleTransformTagXmlEventHandler eventHandler = null;
        try{
            eventHandler = (SimpleTransformTagXmlEventHandler)assertTagIsRegistered(before, rule);
        }
        catch (ClassCastException cce){
            assertThat("The transformer is not SimpleTransformTagXmlEventHandler", false);
        }
        assertThat("The replacement tag is not registered properly", eventHandler.getNewElement(), equalTo(after));

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
