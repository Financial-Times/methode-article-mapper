package com.ft.methodearticletransformer.transformation;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

import org.apache.commons.lang.StringUtils;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.xml.eventhandlers.BaseXMLParser;
import com.ft.bodyprocessing.xml.eventhandlers.UnexpectedElementStructureException;
import com.ft.bodyprocessing.xml.eventhandlers.XmlParser;

public class SlideshowXMLParser extends BaseXMLParser<SlideshowData> implements XmlParser<SlideshowData> {

    private static final String DEFAULT_ELEMENT_NAME = "a";
    private static final QName HREF_QNAME = QName.valueOf("href");
    private static final String UUID_KEY = "uuid";
    
    protected SlideshowXMLParser() {
        super(DEFAULT_ELEMENT_NAME);
    }
    
    /**
     * Overloaded element name passed in using this constructor.
     * @param startElementName
     */
    protected SlideshowXMLParser(String startElementName) {
        super(startElementName);
    }

    @Override
    public SlideshowData createDataBeanInstance() {
        return new SlideshowData();
    }

    @Override
    public void populateBean(SlideshowData dataBean, StartElement nextStartElement, XMLEventReader xmlEventReader) throws UnexpectedElementStructureException {
        Attribute hrefElement = nextStartElement.getAttributeByName(HREF_QNAME);
        // Ensure the element contains an HREF attribute
        if(hrefElement != null) {
            String[] attributesSides = StringUtils.splitPreserveAllTokens(hrefElement.getValue(), "?");
            // Ensure that the href contains at least 1 query parameter
            if(attributesSides.length == 2) {
                // Split all query (key/value) parameters found
                String[] attributes = StringUtils.splitPreserveAllTokens(attributesSides[1], "&");
                
                // Search for the UUID (key/value) parameter, ignore all others
                for(String attribute: attributes){
                    String[] keyValue = StringUtils.splitPreserveAllTokens(attribute, "=");
                    if(UUID_KEY.equalsIgnoreCase(keyValue[0])){
                        // ensure there's a key AND a value for the UUID before populating the bean with the UUID data
                        if(keyValue.length == 2){
                            dataBean.setUuid(keyValue[1]);
                        }
                    }
                }
            }
        }
    }
    
    @Override
    public boolean doesTriggerElementContainAllDataNeeded() {
        return false;
    }

    @Override
    public void transformFieldContentToStructuredFormat(SlideshowData dataBean,
            BodyProcessingContext bodyProcessingContext) {
        // Do nothing, no need for further data processing.
    }


}
