package com.ft.methodearticletransformer.transformation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.richcontent.RichContentItem;
import com.ft.bodyprocessing.richcontent.Video;
import com.ft.bodyprocessing.richcontent.VideoMatcher;
import com.ft.bodyprocessing.richcontent.VideoSiteConfiguration;
import com.ft.bodyprocessing.writer.BodyWriter;
import com.ft.bodyprocessing.xml.eventhandlers.BaseXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandler;

public class MethodeOtherVideoXmlEventHandler extends BaseXMLEventHandler {
    private static final String NEW_ELEMENT = "a";
    private static final String NEW_ELEMENT_ATTRIBUTE = "href";
    private static final String DATA_EMBEDDED = "data-embedded";
    private static final String TRUE = "true";
    private static final String DATA_ASSET_TYPE = "data-asset-type";
    private static final String VIDEO = "video";
    private final XMLEventHandler fallbackHandler;

    public static List<VideoSiteConfiguration> DEFAULTS = Arrays.asList(
            new VideoSiteConfiguration("https?://www.youtube.com/watch\\?v=(?<id>[A-Za-z0-9_-]+)", null, true),
            new VideoSiteConfiguration("https?://www.youtube.com/embed/(?<id>[A-Za-z0-9_-]+)", "https://www.youtube.com/watch?v=%s", false),
            new VideoSiteConfiguration("https?://youtu.be/(?<id>[A-Za-z0-9_-]+)", "https://www.youtube.com/watch?v=%s", false),
            new VideoSiteConfiguration("https?://vimeo.com/[0-9]+", null, false),
            new VideoSiteConfiguration("//player.vimeo.com/video/(?<id>[0-9]+)", "http://www.vimeo.com/%s", true)
    );

    public MethodeOtherVideoXmlEventHandler(XMLEventHandler fallbackHandler) {
        this.fallbackHandler = fallbackHandler;
    }

    @Override
    public void handleStartElementEvent(StartElement event, XMLEventReader xmlEventReader, BodyWriter eventWriter,
                                        BodyProcessingContext bodyProcessingContext) throws XMLStreamException {

        Attribute srcAttribute = event.asStartElement().getAttributeByName(QName.valueOf("src"));

        if(srcAttribute == null){
            fallbackHandler.handleStartElementEvent(event, xmlEventReader, eventWriter, bodyProcessingContext);
            return;
        }

        Video video = convertToVideo(srcAttribute);

        if(video==null) {
            fallbackHandler.handleStartElementEvent(event, xmlEventReader, eventWriter, bodyProcessingContext);
            return;
        }

        Map<String, String> attributesToAdd = new HashMap<>();
        attributesToAdd.put(NEW_ELEMENT_ATTRIBUTE, video.getUrl());
        attributesToAdd.put(DATA_EMBEDDED, TRUE);
        attributesToAdd.put(DATA_ASSET_TYPE, VIDEO);

        eventWriter.writeStartTag(NEW_ELEMENT, attributesToAdd);
        eventWriter.writeEndTag(NEW_ELEMENT);

    }

    public Video convertToVideo(Attribute srcAttribute) {
        String videoLink =  srcAttribute.getValue();
        RichContentItem attachment = new RichContentItem(videoLink, null);
        VideoMatcher matcher = new VideoMatcher(DEFAULTS);
        Video video = matcher.filterVideo(attachment);
        return video;
    }

    @Override
    public void handleEndElementEvent(EndElement event, XMLEventReader xmlEventReader, BodyWriter eventWriter) throws XMLStreamException {
        //only a fallback one should hit this code.
        fallbackHandler.handleEndElementEvent(event, xmlEventReader, eventWriter);
    }

}
