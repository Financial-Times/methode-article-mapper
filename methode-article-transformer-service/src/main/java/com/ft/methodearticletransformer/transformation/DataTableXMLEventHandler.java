package com.ft.methodearticletransformer.transformation;

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

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class DataTableXMLEventHandler extends BaseXMLEventHandler {

	private static final String DATA_TABLE_ATTRIBUTE_VALUE = "data-table";
	private static final String DATA_TABLE_ATTRIBUTE_NAME = "class";
	private static final String DATA_TABLE_HTML_ELEMENT_NAME = "table";
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
				// process raw data and add any assets to the context
				dataTableDataXmlParser.transformFieldContentToStructuredFormat(dataBean, bodyProcessingContext);

				// ensure that the mutated bean data is still valid for processing after the transform field content processing
				if(dataBean.isAllRequiredDataPresent()) {
					eventWriter.writeStartTag(DATA_TABLE_HTML_ELEMENT_NAME, dataTableAttribute());
					eventWriter.writeRaw(dataBean.getBody());
					eventWriter.writeEndTag(DATA_TABLE_HTML_ELEMENT_NAME);
				}
			}

		} else {
			stripElementAndContentsXMLEventHandler.handleStartElementEvent(startElement, xmlEventReader, eventWriter, bodyProcessingContext);
		}
	}

	private Map<String, String> dataTableAttribute() {
		Map<String, String> attributes = new HashMap<>();
		attributes.put(DATA_TABLE_ATTRIBUTE_NAME, DATA_TABLE_ATTRIBUTE_VALUE);
		return attributes;
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

}
