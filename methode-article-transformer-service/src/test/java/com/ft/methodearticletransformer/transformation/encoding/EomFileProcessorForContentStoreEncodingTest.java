package com.ft.methodearticletransformer.transformation.encoding;

import static com.ft.methodearticletransformer.transformation.EomFileProcessorForContentStoreTest.createStandardEomFileWithMainImage;
import static com.ft.methodearticletransformer.transformation.EomFileProcessorForContentStoreTest.FINANCIAL_TIMES_BRAND;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import com.ft.content.model.Brand;
import com.ft.content.model.Content;
import com.ft.methodeapi.model.EomFile;
import com.ft.methodearticletransformer.transformation.EomFileProcessorForContentStore;
import com.ft.methodearticletransformer.transformation.FieldTransformer;
import com.ft.methodearticletransformer.util.ImageSetUuidGenerator;
import org.junit.Before;
import org.junit.Test;


public class EomFileProcessorForContentStoreEncodingTest {
    private static final String TRANSFORMED_BYLINE = "By Gillian Tett";
    private static final String TRANSACTION_ID = "tid_test";
    
    private FieldTransformer bodyTransformer = mock(FieldTransformer.class);
    private FieldTransformer bylineTransformer = mock(FieldTransformer.class);
    private Brand financialTimesBrand = new Brand(FINANCIAL_TIMES_BRAND);
    
    private final UUID uuid = UUID.randomUUID();
    
    private EomFileProcessorForContentStore eomFileProcessorForContentStore =
            new EomFileProcessorForContentStore(bodyTransformer, bylineTransformer, financialTimesBrand);
    
    @Before
    public void setUp() throws Exception {
        when(bylineTransformer.transform(anyString(), anyString())).thenReturn(TRANSFORMED_BYLINE);
    }
    
    @Test
    public void thatCharacterEncodingIsPreserved() {
        final String bodyText = "<p>Gina Rinehart, Australia’s richest person, has taken a television channel "
                + "to court in an apparent attempt to block the broadcast of a hit miniseries detailing her "
                + "colourful family and business history.</p>";
        
        when(bodyTransformer.transform(anyString(), anyString()))
            .thenReturn(String.format("<body>%s</body>", bodyText));
        
        final UUID imageUuid = UUID.randomUUID();
        final UUID expectedMainImageUuid = ImageSetUuidGenerator.fromImageUuid(imageUuid);
        
        String expectedBody = String.format(
                "<body><content data-embedded=\"true\" id=\"%s\" type=\"%s\"></content>%s</body>",
                expectedMainImageUuid, "http://www.ft.com/ontology/content/ImageSet", bodyText);
        
        final EomFile eomFile = createStandardEomFileWithMainImage(uuid, imageUuid, "Primary size");
        Content content = eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID);
        
        assertThat(String.format("body content using JVM encoding %s", System.getProperty("file.encoding")),
                content.getBody(), equalToIgnoringWhiteSpace(expectedBody));
    }
}
