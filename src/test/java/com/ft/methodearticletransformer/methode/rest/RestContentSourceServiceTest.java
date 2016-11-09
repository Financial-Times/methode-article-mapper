package com.ft.methodearticletransformer.methode.rest;

import com.ft.jerseyhttpwrapper.ResilientClientBuilder;
import com.ft.jerseyhttpwrapper.config.EndpointConfiguration;
import com.ft.methodearticletransformer.configuration.AssetTypeRequestConfiguration;
import com.ft.methodearticletransformer.configuration.ConnectionConfiguration;
import com.ft.methodearticletransformer.configuration.SourceApiEndpointConfiguration;
import com.ft.methodearticletransformer.methode.ResourceNotFoundException;
import com.ft.methodearticletransformer.methode.SourceApiUnavailableException;
import com.ft.methodearticletransformer.methode.UnexpectedSourceApiException;
import com.ft.methodearticletransformer.model.EomAssetType;
import com.ft.methodearticletransformer.model.EomFile;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.UrlMatchingStrategy;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.net.HostAndPort;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandler;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;

import org.apache.http.conn.ConnectTimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Matchers;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RestContentSourceServiceTest {

	private static final String TEST_HOST = "localhost";
	private static final String ROOT = "/";
	private static final UUID SAMPLE_UUID = UUID.randomUUID();
	private static final String SAMPLE_TRANSACTION_ID = "tid_test_allieshaveleftus";

	//don't change these without also changing the asset type tests
    private static final int NUMBER_OF_ASSET_TYPE_IDS_PER_REQUEST = 2;
    private static final Integer NUMBER_OF_PARALLEL_ASSET_TYPE_REQUESTS = 4;
    private static final String JSON_EOM_FILE = "{\"uuid\":\"" + SAMPLE_UUID
            + "\",\"type\":\"EOM::CompoundStory\",\"value\":\"\"," +
            "\"attributes\":\"<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"?>\\n<!DOCTYPE ObjectMetadata SYSTEM" +
            "\\\"/SysConfig/Classify/FTStories/classify.dtd\\\"><ObjectMetadata>\\n    <EditorialDisplayIndexing>\\n        " +
            "<DILeadCompanies><DILeadCompany title=\\\"Google Inc\\\"><DICoFTCode>GOOGL00000</DICoFTCode><DICoDescriptor>Google Inc</DICoDescriptor>" +
            "<DICoTickerSymbol>GOOG</DICoTickerSymbol><DICoTickerExchangeCountry>us</DICoTickerExchangeCountry><DICoTickerExchangeCode/>" +
            "<DICoFTMWTickercode>us:GOOG</DICoFTMWTickercode><DICoSEDOL>B020QX2</DICoSEDOL><DICoISIN/><DICoCOFlag/><DICoVersion/></DILeadCompany>" +
            "</DILeadCompanies>\\n        <DITemporaryCompanies>\\n            <DITemporaryCompany>\\n                <DICoTempCode/>\\n                " +
            "<DICoTempDescriptor/>\\n                <DICoTickerCode/>\\n            </DITemporaryCompany>\\n        </DITemporaryCompanies>\\n        " +
            "<DIFTSEGlobalClassifications/>\\n        <DIStockExchangeIndices/>\\n        <DIHotTopics/>\\n        " +
            "<DIHeadlineCopy>Lead headline Â£42m for S&amp;Pâ\u0080\u0099s â\u0080\u009Cup 79%â\u0080\u009D</DIHeadlineCopy>\\n        " +
            "<DIBylineCopy>By </DIBylineCopy>\\n        \\n    <DIFTNPSections/></EditorialDisplayIndexing>\\n    <OutputChannels>\\n        <DIFTN>\\n" +
            "            <DIFTNPublicationDate/>\\n            <DIFTNZoneEdition/>\\n            <DIFTNPage/>\\n            <DIFTNTimeEdition/>\\n" +
            "            <DIFTNFronts/>\\n        </DIFTN>\\n        <DIFTcom>\\n            <DIFTcomWebType>story</DIFTcomWebType>\\n            " +
            "<DIFTcomDisplayCodes>\\n                <DIFTcomDisplayCodeRank1/>\\n                <DIFTcomDisplayCodeRank2/>\\n            " +
            "</DIFTcomDisplayCodes>\\n            <DIFTcomSubscriptionLevel>2</DIFTcomSubscriptionLevel>\\n            " +
            "<DIFTcomUpdateTimeStamp>False</DIFTcomUpdateTimeStamp>\\n            <DIFTcomIndexAndSynd>false</DIFTcomIndexAndSynd>\\n            " +
            "<DIFTcomSafeToSyndicate>True</DIFTcomSafeToSyndicate>\\n            <DIFTcomInitialPublication>20131018162433</DIFTcomInitialPublication>\\n" +
            "            <DIFTcomLastPublication>20131125125802</DIFTcomLastPublication>\\n            " +
            "<DIFTcomSuppresInlineAds>False</DIFTcomSuppresInlineAds>\\n            <DIFTcomMap>True</DIFTcomMap>\\n            " +
            "<DIFTcomDisplayStyle>Normal</DIFTcomDisplayStyle>\\n            <DIFTcomMarkDeleted>False</DIFTcomMarkDeleted>\\n            " +
            "<DIFTcomMakeUnlinkable>False</DIFTcomMakeUnlinkable>\\n            <isBestStory>0</isBestStory>\\n            " +
            "<DIFTcomCMRId>762172</DIFTcomCMRId>\\n            <DIFTcomCMRHint/>\\n            <DIFTcomCMR>\\n                <DIFTcomCMRPrimarySection/>" +
            "\\n                <DIFTcomCMRPrimarySectionId/>\\n                <DIFTcomCMRPrimaryTheme/>\\n                <DIFTcomCMRPrimaryThemeId/>\\n" +
            "                <DIFTcomCMRBrand/>\\n                <DIFTcomCMRBrandId/>\\n                <DIFTcomCMRGenre/>\\n                " +
            "<DIFTcomCMRGenreId/>\\n                <DIFTcomCMRMediaType/>\\n                <DIFTcomCMRMediaTypeId/>\\n            </DIFTcomCMR>\\n" +
            "            \\n            \\n            \\n            \\n            \\n            \\n        <DIFTcomECPositionInText>Default" +
            "</DIFTcomECPositionInText><DIFTcomHideECLevel1>False</DIFTcomHideECLevel1><DIFTcomHideECLevel2>False</DIFTcomHideECLevel2>" +
            "<DIFTcomHideECLevel3>False</DIFTcomHideECLevel3><DIFTcomDiscussion>True</DIFTcomDiscussion><DIFTcomArticleImage>Primary size" +
            "</DIFTcomArticleImage></DIFTcom>\\n        <DISyndication>\\n            <DISyndBeenCopied>False</DISyndBeenCopied>\\n            " +
            "<DISyndEdition>USA</DISyndEdition>\\n            <DISyndStar>01</DISyndStar>\\n            <DISyndChannel/>\\n            <DISyndArea/>\\n" +
            "            <DISyndCategory/>\\n        </DISyndication>\\n    </OutputChannels>\\n    <EditorialNotes>\\n        <Language>English</Language>" +
            "\\n        <Author>roddamm</Author>\\n        <Guides/>\\n        <Editor/>\\n        <Sources>\\n            \\n        " +
            "<Source title=\\\"Financial Times\\\"><SourceCode>FT</SourceCode><SourceDescriptor>Financial Times</SourceDescriptor>" +
            "<SourceOnlineInclusion>True</SourceOnlineInclusion><SourceCanBeSyndicated>True</SourceCanBeSyndicated></Source></Sources>\\n        " +
            "<WordCount>2644</WordCount>\\n        <CreationDate>20131018133645</CreationDate>\\n        <EmbargoDate/>\\n        " +
            "<ExpiryDate>20131018133645</ExpiryDate>\\n        <ObjectLocation>/FT/Content/World News/Stories/Live/TestForSteveAshdown.xml</ObjectLocation>\\n" +
            "        <OriginatingStory/>\\n        <CCMS>\\n            <CCMSCommissionRefNo/>\\n            <CCMSContributorRefNo/>\\n            " +
            "<CCMSContributorFullName/>\\n            <CCMSContributorInclude/>\\n            <CCMSContributorRights/>\\n            <CCMSFilingDate/>\\n" +
            "            <CCMSProposedPublishingDate/>\\n        </CCMS>\\n    </EditorialNotes>\\n    <WiresIndexing>\\n        <category/>\\n        <Keyword/>" +
            "\\n        <char_count/>\\n        <priority/>\\n        <basket/>\\n        <title/>\\n        <Version/>\\n        <story_num/>\\n        " +
            "<file_name/>\\n        <serviceid/>\\n        <entry_date/>\\n        <ref_field/>\\n        <take_num/>\\n    </WiresIndexing>\\n    " +
            "\\n<DataFactoryIndexing><ADRIS_MetaData><IndexSuccess>yes</IndexSuccess><StartTime>Mon Nov 25 12:58:03 GMT 2013</StartTime>" +
            "<EndTime>Mon Nov 25 12:58:03 GMT 2013</EndTime></ADRIS_MetaData><DFMajorCompanies/><DFMinorCompanies/><DFNAICS/><DFWPMIndustries/>" +
            "<DFFTSEGlobalClassifications/><DFStockExchangeIndices/><DFSubjects/><DFCountries/><DFRegions/><DFWPMRegions/><DFProvinces/><DFFTcomDisplayCodes/>" +
            "<DFFTSections/><DFWebRegions/></DataFactoryIndexing></ObjectMetadata>\",\"workflowStatus\":\"Stories/WebRevise\"," +
            "\"systemAttributes\":\"<props>" +
            "<productInfo><name>FTcom</name>\\n<issueDate>20131018</issueDate>\\n</productInfo>\\n<workFolder>/FT/WorldNews</workFolder>\\n<templateName>" +
            "/SysConfig/Templates/FT/Base-Story.xml</templateName>\\n<summary>Text Formatting\\n\\nThis is an example of bold text : This text is bold &lt;b&gt;" +
            "\\n\\nThis is an example of italic text : This text is italic &lt;i&gt;\\n\\nEmphasis\\n\\nStrong\\n\\nSuperscript\\n\\nSubscript\\n\\nUnderline" +
            "\\n\\n\\nLinks\\n\\nThis is a link for an FT article that has been cut and paste from a browser: http://www.ft.com/cms/s/2/e78a8668-c997-11e1-aae2-002128161462.html." +
            "\\nThis link was added using Right-click, Insert Hyperlink: A story about something financial\\n\\nThis link was added using drag and drop of an article: A story abo..." +
            "</summary><wordCount>2644</wordCount></props>\",\"webUrl\":\"http://www.ft.com/a-url\"}";
    @ClassRule
	public static WireMockClassRule sourceApiWireMockRule = new WireMockClassRule(0); //will allocate a free port
	@Rule
	public WireMockClassRule sourceApiInstanceRule = sourceApiWireMockRule;
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	private ObjectMapper objectMapper;
	private SourceApiEndpointConfiguration sourceApiEndpointConfiguration;
	private Environment environment;
	private ClientHandler handler = mock(ClientHandler.class);
    private Client mockClient = new Client(handler);
    private ClientResponse clientResponse = mock(ClientResponse.class);
	private RestContentSourceService restMethodeFileService;
	private RestContentSourceService mockedClientRestMethodeFileService;

	@Before
	public void setup() {
	    objectMapper = new ObjectMapper();

		JerseyClientConfiguration fastTimeOuts = new JerseyClientConfiguration();
		fastTimeOuts.setConnectionTimeout(Duration.milliseconds(100));
		fastTimeOuts.setTimeout(Duration.milliseconds(100));

		int port = sourceApiInstanceRule.port();

		EndpointConfiguration endpointConfiguration = new EndpointConfiguration(
				Optional.of("methode-file-service-test"),
				Optional.of(fastTimeOuts),
				Optional.of(ROOT),
				Arrays.asList(String.format("%s:%d:%d", TEST_HOST, port, port + 1)),
				Collections.<String>emptyList());

		AssetTypeRequestConfiguration assetTypeRequestConfiguration =
		        new AssetTypeRequestConfiguration(NUMBER_OF_ASSET_TYPE_IDS_PER_REQUEST, NUMBER_OF_PARALLEL_ASSET_TYPE_REQUESTS);

        ConnectionConfiguration connectionConfig = new ConnectionConfiguration(3, 1000);

		sourceApiEndpointConfiguration =
				new SourceApiEndpointConfiguration(endpointConfiguration, assetTypeRequestConfiguration, connectionConfig);

		environment = new Environment("test-env", null, null, new MetricRegistry(), Thread.currentThread().getContextClassLoader());

		HostAndPort endpoint = HostAndPort.fromParts("localhost", port);
		Client client = ResilientClientBuilder.inTesting(endpoint).build();

		restMethodeFileService = new RestContentSourceService(environment, client, sourceApiEndpointConfiguration);

		//for cases where we want to control what the client does
        when(clientResponse.getStatus()).thenReturn(200);
        when(clientResponse.getEntity(Matchers.<GenericType<Map<String, EomAssetType>>>any())).thenReturn(new HashMap<String, EomAssetType>());
		mockedClientRestMethodeFileService = new RestContentSourceService(environment, mockClient, sourceApiEndpointConfiguration);
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
        Set<String> assetIds = Sets.newHashSet("test1", "test2", "test3", "test4", "test5");

        Map<String, EomAssetType> expectedAssetTypes = stubResponsesAndGetExpectedAssetTypes(assetIds);
        Map<String, EomAssetType> assetTypes = restMethodeFileService.assetTypes(assetIds, SAMPLE_TRANSACTION_ID);
        assertNotNull(assetTypes);
        assertEquals(expectedAssetTypes, assetTypes);
    }

    @Test
    public void shouldSuccessfullyGetAssetTypesSplitBetweenMoreRequestsThanNumberOfThreads() throws Exception {
        Set<String> assetIds = Sets.newHashSet("test1", "test2", "test3", "test4", "test5",
                "test6", "test7", "test8", "test9");
        Map<String, EomAssetType> expectedAssetTypes = stubResponsesAndGetExpectedAssetTypes(assetIds);
        Map<String, EomAssetType> assetTypes = restMethodeFileService.assetTypes(assetIds, SAMPLE_TRANSACTION_ID);
        assertNotNull(assetTypes);
        assertEquals(expectedAssetTypes, assetTypes);
    }

	@Test
	public void shouldReturnValidEomFileWhenFileFound() {
		stubFor(get(toFindEomFileUrl()).willReturn(anEomFileResponse(200)));

		EomFile eomFile = restMethodeFileService.fileByUuid(SAMPLE_UUID, SAMPLE_TRANSACTION_ID);
		assertThat(eomFile, is(notNullValue()));
		assertThat(eomFile.getUuid(), is(SAMPLE_UUID.toString()));
		assertThat(eomFile.getWorkflowStatus(), is(equalTo("Stories/WebRevise")));
		assertThat(eomFile.getAttributes(), containsString("20131018133645"));
		assertThat(eomFile.getType(), is(equalTo("EOM::CompoundStory")));
		assertThat(eomFile.getSystemAttributes(), containsString("templateName"));
		assertThat(eomFile.getValue(), is(notNullValue()));
        assertThat(eomFile.getWebUrl(), is(equalTo(URI.create("http://www.ft.com/a-url"))));
    }

	@Test
	public void shouldThrowFileNotFoundExceptionWhen404RetrievingEomFile() {
	    stubFor(get(toFindEomFileUrl()).willReturn(anEomFileResponse(404)));
	    expectedException.expect(ResourceNotFoundException.class);
	    expectedException.expect(hasProperty("uuid", equalTo(SAMPLE_UUID)));
	    restMethodeFileService.fileByUuid(SAMPLE_UUID, SAMPLE_TRANSACTION_ID);
	}

	@Test
	public void shouldThrowSourceApiUnavailableExceptionWhen503RetrievingEomFile() {
		stubFor(get(toFindEomFileUrl()).willReturn(anEomFileResponse(503)));
		expectedException.expect(SourceApiUnavailableException.class);
		restMethodeFileService.fileByUuid(SAMPLE_UUID, SAMPLE_TRANSACTION_ID);
	}

    @Test
    public void shouldThrowUnexpectedSourceApiExceptionWhen500RetrievingEomFile() {
        stubFor(get(toFindEomFileUrl()).willReturn(anEomFileResponse(500)));
        expectedException.expect(UnexpectedSourceApiException.class);
        restMethodeFileService.fileByUuid(SAMPLE_UUID, SAMPLE_TRANSACTION_ID);
    }

	@Test
    public void shouldThrowDistinctExceptionForSocketTimeout() {
        when(handler.handle(any(ClientRequest.class))).thenThrow( new ClientHandlerException(new SocketTimeoutException()));
        expectedException.expect(SourceApiUnavailableException.class);
        mockedClientRestMethodeFileService.fileByUuid(SAMPLE_UUID, SAMPLE_TRANSACTION_ID);
    }

    @Test
    public void shouldThrowDistinctExceptionForAnyOtherIssueWithTheTcpSocket() {
        when(handler.handle(any(ClientRequest.class))).thenThrow( new ClientHandlerException(new SocketException()));
        expectedException.expect(SourceApiUnavailableException.class);
        mockedClientRestMethodeFileService.fileByUuid(SAMPLE_UUID, SAMPLE_TRANSACTION_ID);
    }

    @Test
    public void shouldThrowDistinctExceptionForConnectionTimeout() {
        when(handler.handle(any(ClientRequest.class))).thenThrow( new ClientHandlerException(new ConnectTimeoutException()));
        expectedException.expect(SourceApiUnavailableException.class);
        mockedClientRestMethodeFileService.fileByUuid(SAMPLE_UUID, SAMPLE_TRANSACTION_ID);
    }

	@Test
    public void shouldThrowSourceApiUnavailableExceptionWhen503RetrievingAssetTypes() throws Exception {
	    Map<String, EomAssetType> expectedAssetTypes = getExpectedAssetTypesForSlice(Lists.newArrayList("test1"));
        stubFor(post(toFindAssetTypesUrl()).willReturn(anAssetTypeResponseForExpectedOutput(expectedAssetTypes, 503)));

        expectedException.expect(SourceApiUnavailableException.class);

        restMethodeFileService.assetTypes(Sets.newHashSet("test1", "test2", "test3", "test4", "test5"), SAMPLE_TRANSACTION_ID);
    }

	//one of the requests fails with socket timeout, get ApiNetworkingException
    @Test
	public void shouldThrowApiNetworkingExceptionForGetAssetTypesWhenOneRequestFailsWithSocketTimeout() {
        when(handler.handle(any(ClientRequest.class))).thenReturn(clientResponse).thenReturn(clientResponse)
            .thenThrow( new ClientHandlerException(new SocketTimeoutException("socket timeout")));
        expectedException.expect(SourceApiUnavailableException.class);
        mockedClientRestMethodeFileService.assetTypes(Sets.newHashSet("test1", "test2", "test3", "test4", "test5"), SAMPLE_TRANSACTION_ID);
    }

    //one of the requests fails with connect timeout, get ApiNetworkingException
    @Test
    public void shouldThrowDistinctExceptionForGetAssetTypesWhenOneRequestFailsWithConnectTimeout() {
        when(handler.handle(any(ClientRequest.class))).thenReturn(clientResponse).thenReturn(clientResponse)
            .thenThrow( new ClientHandlerException(new ConnectTimeoutException("connect timeout")));
        expectedException.expect(SourceApiUnavailableException.class);
        mockedClientRestMethodeFileService.assetTypes(Sets.newHashSet("test1", "test2", "test3", "test4", "test5"), SAMPLE_TRANSACTION_ID);
    }

    //two requests fail with different exceptions, get ApiNetworkingException for one of the failures
    @Test
    public void shouldThrowDistinctExceptionForGetAssetTypesWhenTwoRequestsFail() {
        when(handler.handle(any(ClientRequest.class))).thenReturn(clientResponse)
            .thenThrow( new ClientHandlerException(new SocketTimeoutException("socket timeout")))
            .thenThrow( new ClientHandlerException(new ConnectTimeoutException("connect timeout")));
        expectedException.expect(SourceApiUnavailableException.class);
        mockedClientRestMethodeFileService.assetTypes(Sets.newHashSet("test1", "test2", "test3", "test4", "test5"), SAMPLE_TRANSACTION_ID);
    }

	private UrlMatchingStrategy toFindEomFileUrl() {
		return urlMatching("/methode/.*");
	}

	private UrlMatchingStrategy toFindAssetTypesUrl() {
        return urlMatching("/asset-type");
    }

	private ResponseDefinitionBuilder anEomFileResponse(int code) {
		return aResponse().withStatus(code).withHeader("Content-type", "application/json").withBody(JSON_EOM_FILE);
	}

    private Map<String, EomAssetType> stubResponsesAndGetExpectedAssetTypes(Set<String> assetIds) throws Exception {
        List<List<String>> partitionedAssetIdentifiers = Lists.partition(Lists.newArrayList(assetIds), NUMBER_OF_PARALLEL_ASSET_TYPE_REQUESTS);

        Map<String, EomAssetType> expectedOutput = Maps.newHashMap();

        for (List<String> slice: partitionedAssetIdentifiers) {
            Map<String, EomAssetType> expectedOutputForSlice = getExpectedAssetTypesForSlice(slice);
            expectedOutput.putAll(expectedOutputForSlice);
            stubFor(post(toFindAssetTypesUrl()).willReturn(anAssetTypeResponseForExpectedOutput(expectedOutput, 200)));
        }

        return expectedOutput;
    }

    private ResponseDefinitionBuilder anAssetTypeResponseForExpectedOutput(Map<String, EomAssetType> expectedOutput, int statusCode) throws Exception {
        return aResponse().withStatus(statusCode).withHeader("Content-type", "application/json").withBody(objectMapper.writeValueAsString(expectedOutput));
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
