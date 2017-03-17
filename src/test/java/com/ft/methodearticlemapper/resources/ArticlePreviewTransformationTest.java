package com.ft.methodearticlemapper.resources;

import com.ft.api.util.transactionid.TransactionIdUtils;
import com.ft.common.FileUtils;
import com.ft.content.model.Brand;
import com.ft.content.model.Content;
import com.ft.methodearticlemapper.exception.UnsupportedEomTypeException;
import com.ft.methodearticlemapper.methode.ContentSource;
import com.ft.methodearticlemapper.model.EomFile;
import com.ft.methodearticlemapper.transformation.EomFileProcessor;
import com.ft.methodearticlemapper.transformation.FieldTransformer;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.HttpHeaders;

import static com.jcabi.matchers.RegexMatchers.matchesPattern;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests article preview transformation logic.
 *
 * Created by julia.fernee on 29/01/2016.
 */
public class ArticlePreviewTransformationTest {

    public static final String ARBITRARY_BRAND = "any brand";
    private static final boolean IS_PREVIEW = true;
    private static final String TRANSACTION_ID = "tid_test";
    private static final String VALID_EOM_FILE_TYPE = "EOM::CompoundStory";
    private static final String VALUE_PROPERTY = FileUtils.readFile("preview/article_preview_value.xml");
    private static final String ATTRIBUTES_PROPERTY = FileUtils.readFile("preview/article_preview_attributes.xml");
    private static final String[] WORFLOW_STATUS = new String[] {"Stories/Write", "Stories/Edit"};
    private static String INVALID_EOM_FILE_TYPE = "NOT_COMPOUND_STORY";
    private HttpHeaders httpHeaders= mock(HttpHeaders.class);
    private FieldTransformer bodyTransformer = mock(FieldTransformer.class);
    private FieldTransformer bylineTransformer = mock(FieldTransformer.class);
    private Brand brand= new Brand(ARBITRARY_BRAND);

    /** Classes under test - validation for successful transformation of an article preview occurs in both classes*/
    private EomFileProcessor eomFileProcessor;
    private PostContentToTransformResource postContentToTransformResource;

    @Before
    public void setUp() throws Exception {
        Map<ContentSource, Brand> contentSourceBrandMap = new HashMap<>();
        contentSourceBrandMap.put(ContentSource.FT, new Brand(ARBITRARY_BRAND));

        eomFileProcessor = new EomFileProcessor(bodyTransformer, bylineTransformer, contentSourceBrandMap);
        postContentToTransformResource= new PostContentToTransformResource(eomFileProcessor);

        when(httpHeaders.getRequestHeader(TransactionIdUtils.TRANSACTION_ID_HEADER)).thenReturn(Arrays.asList(TRANSACTION_ID));
    }

    /**
     * Tests that an unpublished article preview contains minimal required data to transform it to UP content.
     */
    @Test
    public void minimalReqirementsMetForContentPreview() {
        UUID expectedUuid = UUID.randomUUID();
        EomFile testEomFile = articlePreviewMinimalEomFile(expectedUuid.toString());

        Content actualContent = postContentToTransformResource.map(testEomFile, IS_PREVIEW, httpHeaders);

        assertThat(expectedUuid.toString(), equalTo(actualContent.getUuid()));
        assertThat(TRANSACTION_ID, equalTo(actualContent.getPublishReference()));
        assertThat(actualContent.getBrands(), Matchers.contains(brand));
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
        eomFileProcessor.processPreview(eomFile, TRANSACTION_ID, new Date());
    }

    /* In article preview we don't care about systemAttributes, usageTickets & lastModified date */
    static EomFile articlePreviewMinimalEomFile(String uuid) {
        return new EomFile(uuid,
                VALID_EOM_FILE_TYPE,
                VALUE_PROPERTY.getBytes(),
                ATTRIBUTES_PROPERTY,
                WORFLOW_STATUS[0],
                null, null, null);
    }
}
