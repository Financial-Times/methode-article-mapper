package com.ft.methodearticlemapper.transformation;


import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.writer.BodyWriter;
import com.ft.bodyprocessing.xml.eventhandlers.BaseXMLEventHandler;
import com.ft.methodearticlemapper.util.ImageSetUuidGenerator;
import java.util.HashMap;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageSetXmlEventHandler extends BaseXMLEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageSetXmlEventHandler.class);

    private static final String ID_ATTRIBUTE = "id";
    private static final String FT_CONTENT_TAG = "ft-content";

    @Override
    public void handleStartElementEvent(StartElement event, XMLEventReader xmlEventReader, BodyWriter eventWriter,
                                        BodyProcessingContext bodyProcessingContext) throws XMLStreamException {
        String imageID = getIdFromImageSet(event);

        if (StringUtils.isNotEmpty(imageID)) {
            String generatedUUID = ImageSetUuidGenerator.fromImageSetID(imageID).toString();

            HashMap<String, String> attributes = new HashMap<>();
            attributes.put("type", "http://www.ft.com/ontology/content/ImageSet");
            attributes.put("url", "http://api.ft.com/content/" + generatedUUID);
            attributes.put("data-embedded", "true");

            eventWriter.writeStartTag(FT_CONTENT_TAG, attributes);
            eventWriter.writeEndTag(FT_CONTENT_TAG);
        }

        skipUntilMatchingEndTag(event.getName().getLocalPart(), xmlEventReader);
    }

    private String getIdFromImageSet(StartElement event) {
        Attribute idAttribute = event.getAttributeByName(new QName(ID_ATTRIBUTE));
        if (idAttribute == null || StringUtils.isBlank(idAttribute.getValue())) {
            LOGGER.warn("No id attribute or blank for {} required to generate uuid", event.getName().getLocalPart());
            return null;
        }

        return idAttribute.getValue();
    }
}