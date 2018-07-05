package com.ft.methodearticlemapper.transformation;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.writer.BodyWriter;
import com.ft.bodyprocessing.xml.eventhandlers.BaseXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.StripElementAndContentsXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XmlParser;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class DataTableXMLEventHandler extends BaseXMLEventHandler {

	private static final String DATA_TABLE_ATTRIBUTE_VALUE = "data-table";
	private static final String DATA_TABLE_ATTRIBUTE_NAME = "class";
	private static final String DATA_TABLE_HTML_ELEMENT_NAME = "table";
    private static final String P_TAG = "p";
    private static final String TABLE_ID = "id";
    private static final String DATA_TABLE_NAME_ATTRIBUTE = "data-table-name";
    private static final String DATA_TABLE_THEME_ATTRIBUTE = "data-table-theme";
    private static final String DATA_TABLE_COLLAPSE_ROWNUM_ATTRIBUTE = "data-table-collapse-rownum";
    private static final String DATA_TABLE_LAYOUT_SMALLSCREEN_ATTRIBUTE = "data-table-layout-smallscreen";
    private static final String DATA_TABLE_LAYOUT_LARGESCREEN_ATTRIBUTE = "data-table-layout-largescreen";

	private XmlParser<DataTableData> dataTableDataXmlParser;
	private StripElementAndContentsXMLEventHandler stripElementAndContentsXMLEventHandler;

	public DataTableXMLEventHandler(XmlParser<DataTableData> dataTableDataXmlParser,
									StripElementAndContentsXMLEventHandler stripElementAndContentsXMLEventHandler){
		checkNotNull(dataTableDataXmlParser, "dataTableDataXmlParser cannot be null");
		checkNotNull(stripElementAndContentsXMLEventHandler, "stripElementAndContentsXMLEventHandler cannot be null");

		this.dataTableDataXmlParser = dataTableDataXmlParser;
		this.stripElementAndContentsXMLEventHandler = stripElementAndContentsXMLEventHandler;
	}

	@Override
	public void handleStartElementEvent(StartElement startElement, XMLEventReader xmlEventReader, BodyWriter eventWriter,
										BodyProcessingContext bodyProcessingContext) throws XMLStreamException {

		// Confirm that the startEvent is of the correct type
		if (isElementOfCorrectType(startElement)) {

			// Parse the xml needed to create a bean
			DataTableData dataBean = dataTableDataXmlParser.parseElementData(startElement, xmlEventReader,
					bodyProcessingContext);

			// Add asset to the context and create the aside element if all required data is present
			if (dataBean.isAllRequiredDataPresent()) {
                if (eventWriter.isPTagCurrentlyOpen()) {
                    eventWriter.writeEndTag(P_TAG);
                    writeDataTable(eventWriter, dataBean, startElement);
                    eventWriter.writeStartTag(P_TAG, null);
                } else {
                    writeDataTable(eventWriter, dataBean, startElement);
                }
            }
		} else {
			stripElementAndContentsXMLEventHandler.handleStartElementEvent(startElement, xmlEventReader, eventWriter, bodyProcessingContext);
		}
	}

    private void writeDataTable(BodyWriter eventWriter, DataTableData dataBean, StartElement startElement) {
        eventWriter.writeStartTag(DATA_TABLE_HTML_ELEMENT_NAME, dataColumnAttributes(startElement));
        eventWriter.writeRaw(dataBean.getBody());
        eventWriter.writeEndTag(DATA_TABLE_HTML_ELEMENT_NAME);
    }

	protected boolean isElementOfCorrectType(StartElement event) {
		if(event.getName().getLocalPart().toLowerCase().equals(DATA_TABLE_HTML_ELEMENT_NAME.toLowerCase())){
			Attribute classAttr =  event.getAttributeByName(QName.valueOf(DATA_TABLE_ATTRIBUTE_NAME));
			if(classAttr != null && classAttr.getValue().toLowerCase().equals(DATA_TABLE_ATTRIBUTE_VALUE)){
				return true;
			}
		}
		return false;
	}

	private List<String> createAttributesList() {
		List<String> attributesList = new ArrayList<>();
		attributesList.add(0, TABLE_ID);
		attributesList.add(1, DATA_TABLE_NAME_ATTRIBUTE);
		attributesList.add(2, DATA_TABLE_THEME_ATTRIBUTE);
		attributesList.add(3, DATA_TABLE_COLLAPSE_ROWNUM_ATTRIBUTE);
		attributesList.add(4, DATA_TABLE_LAYOUT_SMALLSCREEN_ATTRIBUTE);
		attributesList.add(5, DATA_TABLE_LAYOUT_LARGESCREEN_ATTRIBUTE);
		return attributesList;
	}

	private Map<String, String> dataColumnAttributes(StartElement startElement) {
		Map<String, String> attributesMap = new HashMap<>();
		List<String> attributesList = createAttributesList();

		attributesMap.put(DATA_TABLE_ATTRIBUTE_NAME, DATA_TABLE_ATTRIBUTE_VALUE);

		for (Iterator i = startElement.getAttributes(); i.hasNext();) {
				Attribute attribute = (Attribute) i.next();
				if (attributesList.contains(attribute.getName().toString()))
					attributesMap.put(attribute.getName().toString(), attribute.getValue());
			}

		return attributesMap;
	}

}
