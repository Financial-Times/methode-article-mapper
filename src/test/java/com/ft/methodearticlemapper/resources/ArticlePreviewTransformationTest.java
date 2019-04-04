package com.ft.methodearticlemapper.resources;

import static com.jcabi.matchers.RegexMatchers.matchesPattern;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.EnumSet;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

import com.ft.bodyprocessing.BodyProcessor;
import com.ft.bodyprocessing.html.Html5SelfClosingTagBodyProcessor;
import com.ft.common.FileUtils;
import com.ft.content.model.Content;
import com.ft.methodearticlemapper.configuration.PropertySource;
import com.ft.methodearticlemapper.exception.UnsupportedEomTypeException;
import com.ft.methodearticlemapper.model.EomFile;
import com.ft.methodearticlemapper.transformation.EomFileProcessor;
import com.ft.methodearticlemapper.transformation.FieldTransformer;
import com.ft.methodearticlemapper.transformation.TransformationMode;

public class ArticlePreviewTransformationTest {

    private static final boolean IS_PREVIEW = true;
    private static final String TRANSACTION_ID = "tid_test";
    private static final String VALID_EOM_FILE_TYPE = "EOM::CompoundStory";
    private static final String VALUE_PROPERTY = FileUtils.readFile("preview/article_preview_value.xml");
    private static final String ATTRIBUTES_PROPERTY = FileUtils.readFile("preview/article_preview_attributes.xml");
    private static final String SYSTEM_ATTRIBUTES_PROPERTY = FileUtils.readFile("preview/article_preview_system_attributes.xml");
    private static final String[] WORKFLOW_STATUS = new String[] {"Stories/Write", "Stories/Edit"};
    private static final String INVALID_EOM_FILE_TYPE = "NOT_COMPOUND_STORY";
    private static final String PUBLISH_REF = "publishReference";
    private static final String API_HOST = "test.api.ft.com";
    private static final String WEB_URL_TEMPLATE = "https://www.ft.com/content/%s";
    private static final String CANONICAL_WEB_URL_TEMPLATE = "https://www.ft.com/content/%s";
    private FieldTransformer bodyTransformer = mock(FieldTransformer.class);
    private FieldTransformer bylineTransformer = mock(FieldTransformer.class);
    private BodyProcessor htmlFieldProcessor = spy(new Html5SelfClosingTagBodyProcessor());

    /** Classes under test - validation for successful transformation of an article preview occurs in both classes*/
    private EomFileProcessor eomFileProcessor;
    private PostContentToTransformResource postContentToTransformResource;

    @Before
    public void setUp() {
        eomFileProcessor = new EomFileProcessor(EnumSet.allOf(TransformationMode.class), bodyTransformer,
                bylineTransformer, htmlFieldProcessor, PUBLISH_REF, API_HOST,
                WEB_URL_TEMPLATE, CANONICAL_WEB_URL_TEMPLATE);
        postContentToTransformResource = new PostContentToTransformResource(eomFileProcessor,
                PropertySource.fromTransaction, PropertySource.fromTransaction, PUBLISH_REF);

        MDC.put("transaction_id", "transaction_id=" + TRANSACTION_ID);
    }

    /**
     * Tests that an unpublished article preview contains minimal required data to transform it to UP content.
     */
    @Test
    public void minimalRequirementsMetForContentPreview() {
        UUID expectedUuid = UUID.randomUUID();
        EomFile testEomFile = articlePreviewMinimalEomFile(expectedUuid.toString());

        Content actualContent = postContentToTransformResource.map(testEomFile, IS_PREVIEW, null);

        assertThat(expectedUuid.toString(), equalTo(actualContent.getUuid()));
        assertThat(TRANSACTION_ID, equalTo(actualContent.getPublishReference()));
        assertThat(actualContent.getBody(), matchesPattern("^<body>.*</body>$"));
    }


    /**
     * Tests that validation fails for an article preview when the EomFile type is not an EOM::CompoundStory.
     */
    @Test (expected = UnsupportedEomTypeException.class)
    public void failWhenInvalidEomFileType() {
        EomFile eomFile = mock(EomFile.class);

        String  randomUuid = UUID.randomUUID().toString();

        when(eomFile.getType()).thenReturn(INVALID_EOM_FILE_TYPE);
        when(eomFile.getUuid()).thenReturn(randomUuid);
        eomFileProcessor.process(eomFile, TransformationMode.PREVIEW, TRANSACTION_ID, new Date());
    }

    /* In article preview we don't care about systemAttributes, usageTickets & lastModified date */
    static EomFile articlePreviewMinimalEomFile(String uuid) {
        return new EomFile(uuid,
                VALID_EOM_FILE_TYPE,
                VALUE_PROPERTY.getBytes(),
                ATTRIBUTES_PROPERTY,
                WORKFLOW_STATUS[0],
                SYSTEM_ATTRIBUTES_PROPERTY, null);
    }
}
