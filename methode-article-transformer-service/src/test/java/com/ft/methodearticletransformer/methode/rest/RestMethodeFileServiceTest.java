package com.ft.methodearticletransformer.methode.rest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ft.jerseyhttpwrapper.ResilientClientBuilder;
import com.ft.jerseyhttpwrapper.config.EndpointConfiguration;
import com.ft.methodeapi.model.EomAssetType;
import com.ft.methodeapi.model.EomFile;
import com.ft.methodearticletransformer.configuration.AssetTypeRequestConfiguration;
import com.ft.methodearticletransformer.configuration.MethodeApiEndpointConfiguration;
import com.ft.methodearticletransformer.methode.MethodeApiUnavailableException;
import com.ft.methodearticletransformer.methode.MethodeFileNotFoundException;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.UrlMatchingStrategy;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sun.jersey.api.client.Client;

import cucumber.api.java.After;

public class RestMethodeFileServiceTest {

	private static final String TEST_HOST = "localhost";
	private static final String ROOT = "/";
	private static final UUID SAMPLE_UUID = UUID.randomUUID();
	private static final String SAMPLE_TRANSACTION_ID = "tid_test_allieshaveleftus";
	
	private final String SYSTEM_ATTRIBUTES = "<props><productInfo><name>FTcom</name>\n" +
            "<issueDate>20131219</issueDate>\n" +
            "</productInfo>\n" +
            "<workFolder>/FT/Companies</workFolder>\n" +
            "<templateName>/SysConfig/Templates/FT/Base-Story.xml</templateName>\n" +
            "<summary>t text text text text text text text text text text text text text text text text\n" +
            " text text text text te...</summary><wordCount>417</wordCount></props>";


	@ClassRule
	public static WireMockClassRule methodeApiWireMockRule = new WireMockClassRule(0); //will allocate a free port

	@Rule
	public WireMockClassRule instanceRule = methodeApiWireMockRule;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	private ObjectMapper objectMapper;

	private RestMethodeFileService restMethodeFileService;

	@Before
	public void setup() {
	    objectMapper = new ObjectMapper();

		JerseyClientConfiguration fastTimeOuts = new JerseyClientConfiguration();
		fastTimeOuts.setConnectionTimeout(Duration.milliseconds(100));
		fastTimeOuts.setTimeout(Duration.milliseconds(100));

		int port = instanceRule.port();

		EndpointConfiguration endpointConfiguration = new EndpointConfiguration(
				Optional.of("methode-file-service-test"),
				Optional.of(fastTimeOuts),
				Optional.of(ROOT),
				Arrays.asList(String.format("%s:%d:%d", TEST_HOST, port, port + 1)),
				Collections.<String>emptyList());

		//TODO make it obvious not to change these values
		AssetTypeRequestConfiguration assetTypeRequestConfiguration = new AssetTypeRequestConfiguration(2, 4);

		MethodeApiEndpointConfiguration methodeApiEndpointConfiguration =
				new MethodeApiEndpointConfiguration(endpointConfiguration, assetTypeRequestConfiguration);

		Environment environment = new Environment("test-env", null, null, new MetricRegistry(), Thread.currentThread().getContextClassLoader());

		Client client = ResilientClientBuilder.in(environment).using(endpointConfiguration).build();

		restMethodeFileService = new RestMethodeFileService(environment, client, methodeApiEndpointConfiguration);
	}
	
	@Test
	public void shouldSuccessfullyRetrieveEomFile() throws Exception {
	    final byte[] fileBytes = "blah, blah, blah".getBytes();
        stubFor(get(toFindEomFileUrl()).willReturn(anEomFileResponseWithBytes(fileBytes)));
        EomFile eomFile = restMethodeFileService.fileByUuid(SAMPLE_UUID, SAMPLE_TRANSACTION_ID);
        assertNotNull(eomFile);
        assertArrayEquals(fileBytes, eomFile.getValue());
	}
	
	@Test
    public void shouldSuccessfullyGetAssetTypesInSingleRequest() throws Exception {
        Set<String> assetIds = Sets.newHashSet("test1", "test2");
        Map<String, EomAssetType> expectedAssetTypes = stubResponsesAndGetExpectedAssetTypes(assetIds);
        Map<String, EomAssetType> assetTypes = restMethodeFileService.assetTypes(assetIds, SAMPLE_TRANSACTION_ID);
        assertNotNull(assetTypes);
        assertEquals(expectedAssetTypes, assetTypes);
    }
    
    @Test
    public void shouldSuccessfullyGetAssetTypesSplitBetweenFewerRequestsThanNumberOfThreads() throws Exception {
        Set<String> assetIds = Sets.newHashSet("test1", "test2", "test3", "test4", "test5",
                "test6", "test7", "test8", "test9");
        Map<String, EomAssetType> expectedAssetTypes = stubResponsesAndGetExpectedAssetTypes(assetIds);
        Map<String, EomAssetType> assetTypes = restMethodeFileService.assetTypes(assetIds, SAMPLE_TRANSACTION_ID);
        assertNotNull(assetTypes);
        assertEquals(expectedAssetTypes, assetTypes);
    }

    @Test
    public void shouldSuccessfullyGetAssetTypesSplitBetweenMoreRequestsThanNumberOfThreads() throws Exception {
        Set<String> assetIds = Sets.newHashSet("test1", "test2", "test3", "test4", "test5");
        Map<String, EomAssetType> expectedAssetTypes = stubResponsesAndGetExpectedAssetTypes(assetIds);
        Map<String, EomAssetType> assetTypes = restMethodeFileService.assetTypes(assetIds, SAMPLE_TRANSACTION_ID);
        assertNotNull(assetTypes);
        assertEquals(expectedAssetTypes, assetTypes);
    }
    //TODO add exception tests for asset types

	@Test
	public void shouldThrowMethodeFileNotFoundExceptionWhen404FromMethodeApi() {
	       stubFor(get(toFindEomFileUrl()).willReturn(aResponseWithCode(404)));

	        expectedException.expect(MethodeFileNotFoundException.class);
	        expectedException.expect(hasProperty("uuid", equalTo(SAMPLE_UUID)));

	        restMethodeFileService.fileByUuid(SAMPLE_UUID, SAMPLE_TRANSACTION_ID);
	}

	@Test
	public void shouldThrowMethodeApiUnavailableExceptionWhen503FromMethodeApi() {
		stubFor(get(toFindEomFileUrl()).willReturn(aResponseWithCode(503)));

		expectedException.expect(MethodeApiUnavailableException.class);

		restMethodeFileService.fileByUuid(SAMPLE_UUID, SAMPLE_TRANSACTION_ID);
	}

	private UrlMatchingStrategy toFindEomFileUrl() {
		return urlMatching("/eom-file/.*");
	}
	
	private UrlMatchingStrategy toFindAssetTypesUrl() {
        return urlMatching("/asset-type");
    }

	private ResponseDefinitionBuilder aResponseWithCode(int code) {
		return aResponse().withStatus(code).withHeader("Content-type", "application/json");
	}
	
	private ResponseDefinitionBuilder anEomFileResponseWithBytes(byte[] fileBytes) throws Exception {
	    return aResponse().withStatus(200).withHeader("Content-type", "application/json").withBody(eomFileBody(fileBytes));
	}
	
	private String eomFileBody(byte[] fileBytes) throws Exception {
	    EomFile eomFile = new EomFile("asdf", "someType", fileBytes, "some attributes", "WebRevise", SYSTEM_ATTRIBUTES);
	    return objectMapper.writeValueAsString(eomFile);
	}
	
	
	
	
	
	
    private Map<String, EomAssetType> stubResponsesAndGetExpectedAssetTypes(Set<String> assetIds) throws Exception {
        List<List<String>> partitionedAssetIdentifiers = Lists.partition(Lists.newArrayList(assetIds), 4);
        
        Map<String, EomAssetType> expectedOutput = Maps.newHashMap();
        
        for (List<String> slice: partitionedAssetIdentifiers) {
            Map<String, EomAssetType> expectedOutputForSlice = getExpectedAssetTypesForSlice(slice);
            expectedOutput.putAll(expectedOutputForSlice);
            stubFor(post(toFindAssetTypesUrl()).willReturn(anAssetTypeResponseForExpectedOutput(expectedOutput)));
        }
        
        return expectedOutput;
    }
    
    private ResponseDefinitionBuilder anAssetTypeResponseForExpectedOutput(Map<String, EomAssetType> expectedOutput) throws Exception {
        return aResponse().withStatus(200).withHeader("Content-type", "application/json").withBody(objectMapper.writeValueAsString(expectedOutput));
    }

    private Map<String, EomAssetType> getExpectedAssetTypesForSlice(List<String> slice) {
        Map<String, EomAssetType> expectedAssetTypesForSlice = Maps.newHashMap();
        for(String uuid: slice) {
            expectedAssetTypesForSlice.put(uuid, getEomAssetTypeForUuid(uuid));
        }
        return expectedAssetTypesForSlice;
    }

    private EomAssetType getEomAssetTypeForUuid(String uuid) {
        return new EomAssetType.Builder().uuid(uuid).type("EOM:CompoundStory").build();
    }

	@After
	public void cleanUp() {
		reset();
	}

}
