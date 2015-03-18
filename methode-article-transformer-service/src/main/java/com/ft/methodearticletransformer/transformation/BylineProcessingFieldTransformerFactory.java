package com.ft.methodearticletransformer.transformation;

import static java.util.Arrays.asList;

import java.util.List;

import com.ft.bodyprocessing.BodyProcessor;
import com.ft.bodyprocessing.BodyProcessorChain;
import com.ft.bodyprocessing.xml.StAXTransformingBodyProcessor;

public class BylineProcessingFieldTransformerFactory implements FieldTransformerFactory {

    private static final String BY = "By";

	@Override
    public FieldTransformer newInstance() {
        BodyProcessorChain bodyProcessorChain = new BodyProcessorChain(bodyProcessors());
        return new BylineProcessingFieldTransformer(bodyProcessorChain);
    }

    private List<BodyProcessor> bodyProcessors() {
        return asList(stripByAttributesAndValuesBodyProcessor(),
				stAXTransformingBodyProcessor(),
        		new WhitespaceRemovingBodyProcessor(), // get rid of trailing and initial whitespace
        		new RemoveXMLEntityEscapingBodyProcessor(), // convert from XML back to plain text by unencoding escaped characters
        		new RemoveIfExactMatchBodyProcessor(BY));  // if just left with 'By', remove that
    }

    private BodyProcessor stAXTransformingBodyProcessor() {
        return new StAXTransformingBodyProcessor(new MethodeBylineTransformationXMLEventHandlerRegistry());
    }

	private BodyProcessor stripByAttributesAndValuesBodyProcessor() {
		return new StAXTransformingBodyProcessor(new StripByPredefinedAttributesAndValuesEventHandlerRegistry());
	}

}
