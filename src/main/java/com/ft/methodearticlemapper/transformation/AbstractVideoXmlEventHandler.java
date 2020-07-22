package com.ft.methodearticlemapper.transformation;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.writer.BodyWriter;
import com.ft.bodyprocessing.xml.eventhandlers.BaseXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandler;
import com.google.common.base.Strings;
import java.util.HashMap;
import java.util.Map;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;

public abstract class AbstractVideoXmlEventHandler extends BaseXMLEventHandler {

  private static final String CONTENT_TAG = "content";
  private static final String DATA_EMBEDDED = "data-embedded";
  private static final String ID = "id";
  private static final String TYPE = "type";
  private static final String VIDEO_TYPE = "http://www.ft.com/ontology/content/Video";

  private final XMLEventHandler fallbackHandler;

  public AbstractVideoXmlEventHandler(XMLEventHandler fallbackHandler) {
    this.fallbackHandler = fallbackHandler;
  }

  @Override
  public void handleStartElementEvent(
      StartElement event,
      XMLEventReader xmlEventReader,
      BodyWriter eventWriter,
      BodyProcessingContext bodyProcessingContext)
      throws XMLStreamException {
    String videoId = extractVideoId(event);
    if (Strings.isNullOrEmpty(videoId)) {
      fallbackHandler.handleStartElementEvent(
          event, xmlEventReader, eventWriter, bodyProcessingContext);
      return;
    }
    Map<String, String> attributesToAdd = new HashMap<>();
    attributesToAdd.put(ID, videoId);
    attributesToAdd.put(DATA_EMBEDDED, Boolean.TRUE.toString());
    attributesToAdd.put(TYPE, VIDEO_TYPE);
    skipUntilMatchingEndTag(event.getName().toString(), xmlEventReader);
    eventWriter.writeStartTag(CONTENT_TAG, attributesToAdd);
    eventWriter.writeEndTag(CONTENT_TAG);
  }

  public abstract String extractVideoId(StartElement event);
}
