package com.ft.methodearticlemapper.transformation;

import javax.xml.stream.events.StartElement;

public interface StartElementMatcher {
  boolean matches(StartElement element);
}
