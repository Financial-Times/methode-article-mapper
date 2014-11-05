package com.ft.methodearticletransformer.transformation;

import static java.util.Arrays.asList;

import java.util.List;

import com.ft.bodyprocessing.BodyProcessor;
import com.ft.bodyprocessing.BodyProcessorChain;
import com.ft.bodyprocessing.regex.RegexRemoverBodyProcessor;
import com.ft.bodyprocessing.regex.RegexReplacerBodyProcessor;
import com.ft.bodyprocessing.xml.StAXTransformingBodyProcessor;
import com.ft.jerseyhttpwrapper.ResilientClient;
import com.ft.methodearticletransformer.methode.MethodeFileService;

public class BodyProcessingFieldTransformerFactory implements FieldTransformerFactory {

    private final MethodeFileService methodeFileService;
	private ResilientClient semanticStoreContentReaderClient;

	public BodyProcessingFieldTransformerFactory(MethodeFileService methodeFileService, ResilientClient semanticStoreContentReaderClient) {
        this.methodeFileService = methodeFileService;
		this.semanticStoreContentReaderClient = semanticStoreContentReaderClient;
	}

    @Override
    public FieldTransformer newInstance() {
        BodyProcessorChain bodyProcessorChain = new BodyProcessorChain(bodyProcessors());
        return new BodyProcessingFieldTransformer(bodyProcessorChain);
    }

    private List<BodyProcessor> bodyProcessors() {
        return asList(
                new RegexRemoverBodyProcessor("(<p>)\\s*(</p>)|(<p/>)"),
                stAXTransformingBodyProcessor(),
                new RegexRemoverBodyProcessor("(<p>)[\\s(<br/>)]*(</p>)"),
                new RegexReplacerBodyProcessor("(</p>)\\s*(<p>)", "</p><p>"),
                new MethodeLinksBodyProcessor(methodeFileService, semanticStoreContentReaderClient)
        );
    }

    private BodyProcessor stAXTransformingBodyProcessor() {
        return new StAXTransformingBodyProcessor(new MethodeBodyTransformationXMLEventHandlerRegistry());
    }


}
