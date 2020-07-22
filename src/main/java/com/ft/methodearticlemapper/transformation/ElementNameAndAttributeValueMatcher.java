package com.ft.methodearticlemapper.transformation;

import java.util.List;
import javax.xml.stream.events.StartElement;

public interface ElementNameAndAttributeValueMatcher {

  boolean matchesElementNameAndAttributeValueCriteria(
      List<String> attributeValueList, StartElement startElement);
}
