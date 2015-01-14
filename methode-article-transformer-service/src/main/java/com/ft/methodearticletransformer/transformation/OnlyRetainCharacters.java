package com.ft.methodearticletransformer.transformation;

import com.ft.bodyprocessing.xml.eventhandlers.RetainXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.StripXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandlerRegistry;

public class OnlyRetainCharacters extends XMLEventHandlerRegistry {

	public OnlyRetainCharacters() {
		registerDefaultEventHandler(new StripXMLEventHandler());
		registerCharactersEventHandler(new RetainXMLEventHandler());
	}

}
