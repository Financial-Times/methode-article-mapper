package com.ft.methodearticletransformer.transformation;

import javax.xml.stream.events.StartElement;
import java.util.List;

public interface StrikeoutMatcher {

    boolean matchesOnElementName(StartElement startElement);

    boolean matchesStrikeoutCriteria(List<String> attributeValueList, StartElement startElement);
}
