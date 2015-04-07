package com.ft.methodearticletransformer.transformation;

import static java.util.Arrays.asList;

import java.net.URI;
import java.util.List;

import com.ft.bodyprocessing.BodyProcessor;
import com.ft.bodyprocessing.BodyProcessorChain;
import com.ft.bodyprocessing.html.Html5SelfClosingTagBodyProcessor;
import com.ft.bodyprocessing.regex.RegexRemoverBodyProcessor;
import com.ft.bodyprocessing.regex.RegexReplacerBodyProcessor;
import com.ft.bodyprocessing.richcontent.VideoMatcher;
import com.ft.bodyprocessing.xml.StAXTransformingBodyProcessor;
import com.ft.jerseyhttpwrapper.ResilientClient;
import com.ft.methodearticletransformer.methode.MethodeFileService;

public class BodyProcessingFieldTransformerFactory implements FieldTransformerFactory {

    private final MethodeFileService methodeFileService;
	private ResilientClient semanticStoreContentReaderClient;
    private URI uri;
    private VideoMatcher videoMatcher;

	public BodyProcessingFieldTransformerFactory(MethodeFileService methodeFileService, ResilientClient semanticStoreContentReaderClient, URI uri, VideoMatcher videoMatcher) {
        this.methodeFileService = methodeFileService;
		this.semanticStoreContentReaderClient = semanticStoreContentReaderClient;
        this.uri = uri;
        this.videoMatcher = videoMatcher;
	}

    @Override
    public FieldTransformer newInstance() {
        BodyProcessorChain bodyProcessorChain = new BodyProcessorChain(bodyProcessors());
        return new BodyProcessingFieldTransformer(bodyProcessorChain);
    }

    private List<BodyProcessor> bodyProcessors() {
        return asList(
                stripByAttributesAndValuesBodyProcessor(),
                new RegexRemoverBodyProcessor("(<p>)\\s*(</p>)|(<p/>)"),
				new RegexRemoverBodyProcessor("(<p[^>]*?>)\\s*(</p>)|(<p/>)"),
                stAXTransformingBodyProcessor(),
                new RegexRemoverBodyProcessor("(<p>)(\\s|(<br/>))*(</p>)"),
                new RegexReplacerBodyProcessor("</p>(\\r?\\n)+<p>", "</p>" + System.lineSeparator() + "<p>"),
                new RegexReplacerBodyProcessor("</p> +<p>", "</p><p>"),
                new MethodeLinksBodyProcessor(methodeFileService, semanticStoreContentReaderClient, uri),
                new Html5SelfClosingTagBodyProcessor()
        );
    }

    private BodyProcessor stAXTransformingBodyProcessor() {
        return new StAXTransformingBodyProcessor(new MethodeBodyTransformationXMLEventHandlerRegistry(videoMatcher));
    }

    private BodyProcessor stripByAttributesAndValuesBodyProcessor() {
        return new StAXTransformingBodyProcessor(new StripByPredefinedAttributesAndValuesEventHandlerRegistry());
    }
}
