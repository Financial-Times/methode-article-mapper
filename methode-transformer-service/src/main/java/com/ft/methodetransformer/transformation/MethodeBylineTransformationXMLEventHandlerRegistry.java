package com.ft.methodetransformer.transformation;

import com.ft.bodyprocessing.xml.eventhandlers.PlainTextHtmlEntityReferenceEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.RetainXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.StripXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandlerRegistry;

public class MethodeBylineTransformationXMLEventHandlerRegistry extends XMLEventHandlerRegistry {

    public MethodeBylineTransformationXMLEventHandlerRegistry() {

        //strip everything except characters
        registerDefaultEventHandler(new StripXMLEventHandler());
        registerCharactersEventHandler(new RetainXMLEventHandler());
        registerEntityReferenceEventHandler(new PlainTextHtmlEntityReferenceEventHandler());
        // want to be sure to strip the wrapping node
        registerStartAndEndElementEventHandler(new StripXMLEventHandler(), "byline"); 
    }
}
