package com.ft.methodearticletransformer;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.xml.StAXTransformingBodyProcessor;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandlerRegistry;
import com.ft.methodearticletransformer.transformation.MethodeVideoXmlEventHandler;
import org.junit.Test;

public class StAXTransformingBodyProcessorTest {

    private StAXTransformingBodyProcessor bodyProcessor;

    @Test
    public void shouldProcessVideoTagCorrectly() {
        XMLEventHandlerRegistry eventHandlerRegistry = new XMLEventHandlerRegistry() {
            { super.registerStartAndEndElementEventHandler(new MethodeVideoXmlEventHandler("videoID"), "videoPlayer");}
        };
        bodyProcessor = new StAXTransformingBodyProcessor(eventHandlerRegistry);
        String videoText = "<videoPlayer videoID=\"3920663836001\"><web-inline-picture id=\"U2113113643377jlC\" width=\"150\" fileref=\"/FT/Graphics/Online/Z_Undefined/FT-video-story.jpg?uuid=91b39ae8-ccff-11e1-92c1-00144feabdc0\" tmx=\"150 100 150 100\"/>\n" +
                "</videoPlayer>";
        String processedBody = bodyProcessor.process(videoText, new BodyProcessingContext(){});
        assertThat("processedBody", processedBody, is(equalTo("<a href=\"http://video.ft.com/3920663836001\"></a>")));
    }


}
