package com.ft.methodearticlemapper.transformation;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.writer.BodyWriter;
import org.codehaus.stax2.XMLEventReader2;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class RecommendedXMLEventHandlerTest extends BaseXMLEventHandlerTest {

    @Mock
    private BodyWriter mockEventWriter;
    @Mock
    private XMLEventReader2 mockXmlEventReader;
    @Mock
    private BodyProcessingContext mockBodyProcessingContext;


    private RecommendedXMLEventHandler eventHandler;

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testHandleStartElementEvent() throws Exception {
    }
}
