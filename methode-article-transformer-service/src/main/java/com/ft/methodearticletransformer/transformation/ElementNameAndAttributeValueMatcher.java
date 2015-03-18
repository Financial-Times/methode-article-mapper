package com.ft.methodearticletransformer.transformation;

import javax.xml.stream.events.StartElement;
import java.util.List;

public interface ElementNameAndAttributeValueMatcher {

    boolean matchesElementNameAndAttributeValueCriteria(List<String> attributeValueList, StartElement startElement);
}
