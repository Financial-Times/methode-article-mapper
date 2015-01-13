package com.ft.methodearticletransformer;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.xml.StAXTransformingBodyProcessor;
import com.ft.bodyprocessing.xml.eventhandlers.StripElementAndContentsXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandlerRegistry;
import com.ft.methodearticletransformer.transformation.MethodeBrightcoveVideoXmlEventHandler;
import com.ft.methodearticletransformer.transformation.MethodeOtherVideoXmlEventHandler;
import org.junit.Test;

public class StAXTransformingBodyProcessorTest {

    private StAXTransformingBodyProcessor bodyProcessor;

    @Test
    public void shouldProcessVideoTagCorrectly() {
        XMLEventHandlerRegistry eventHandlerRegistry = new XMLEventHandlerRegistry() {
            { super.registerStartAndEndElementEventHandler(new MethodeBrightcoveVideoXmlEventHandler("videoID", new StripElementAndContentsXMLEventHandler()), "videoPlayer");}
        };
        bodyProcessor = new StAXTransformingBodyProcessor(eventHandlerRegistry);
        String videoText = "<videoPlayer videoID=\"3920663836001\"><web-inline-picture id=\"U2113113643377jlC\" width=\"150\" fileref=\"/FT/Graphics/Online/Z_Undefined/FT-video-story.jpg?uuid=91b39ae8-ccff-11e1-92c1-00144feabdc0\" tmx=\"150 100 150 100\"/>\n" +
                "</videoPlayer>";
        String processedBody = bodyProcessor.process(videoText, new BodyProcessingContext(){});
        assertThat("processedBody", processedBody, is(equalTo("<a href=\"http://video.ft.com/3920663836001\"></a>")));
    }



    @Test
    public void shouldProcessYouTubeVideoCorrectly() {
        XMLEventHandlerRegistry eventHandlerRegistry = new XMLEventHandlerRegistry() {
            { super.registerStartAndEndElementEventHandler(new MethodeOtherVideoXmlEventHandler("channel", new StripElementAndContentsXMLEventHandler()), "p");}
        };
        bodyProcessor = new StAXTransformingBodyProcessor(eventHandlerRegistry);
        String videoText = "<p align=\"left\" channel=\"FTcom\">Youtube Video<iframe height=\"245\" frameborder=\"0\" allowfullscreen=\"\" src=\"http://www.youtube.com/embed/YoB8t0B4jx4\" width=\"600\"></iframe>\n" +
                "</p>";
        String processedBody = bodyProcessor.process(videoText, new BodyProcessingContext(){});
        assertThat("processedBody", processedBody, is(equalTo("<p>Youtube Video<a href=\"http://www.youtube.com/embed/YoB8t0B4jx4\"></a></p>")));
    }


}
