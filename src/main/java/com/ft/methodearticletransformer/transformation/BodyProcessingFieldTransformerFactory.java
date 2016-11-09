package com.ft.methodearticletransformer.transformation;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import com.ft.bodyprocessing.BodyProcessingException;
import com.ft.bodyprocessing.BodyProcessor;
import com.ft.bodyprocessing.BodyProcessorChain;
import com.ft.bodyprocessing.html.Html5SelfClosingTagBodyProcessor;
import com.ft.bodyprocessing.regex.RegexRemoverBodyProcessor;
import com.ft.bodyprocessing.regex.RegexReplacerBodyProcessor;
import com.ft.bodyprocessing.richcontent.VideoMatcher;
import com.ft.bodyprocessing.xml.StAXTransformingBodyProcessor;
import com.ft.bodyprocessing.xml.dom.DOMTransformingBodyProcessor;
import com.ft.bodyprocessing.xml.dom.XPathHandler;
import com.ft.jerseyhttpwrapper.ResilientClient;
import com.ft.methodearticletransformer.transformation.xslt.ModularXsltBodyProcessor;
import com.ft.methodearticletransformer.transformation.xslt.XsltFile;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.sun.jersey.api.client.Client;

public class BodyProcessingFieldTransformerFactory implements FieldTransformerFactory {

	private ResilientClient documentStoreApiClient;
	private URI documentStoreUri;
    private VideoMatcher videoMatcher;
    private InteractiveGraphicsMatcher interactiveGraphicsMatcher;
    private final Map<String,XPathHandler> xpathHandlers;
    

	public BodyProcessingFieldTransformerFactory(final ResilientClient documentStoreApiClient,
            final URI uri,
            final VideoMatcher videoMatcher,
            final InteractiveGraphicsMatcher interactiveGraphicsMatcher, Client concordanceApiClient, URI concordanceApiUri) {
		this.documentStoreApiClient = documentStoreApiClient;
        this.documentStoreUri = uri;
        this.videoMatcher = videoMatcher;
        this.interactiveGraphicsMatcher = interactiveGraphicsMatcher;
        xpathHandlers = ImmutableMap.of( "//company", new TearSheetLinksTransformer(concordanceApiClient, concordanceApiUri));
        
	}

    @Override
    public FieldTransformer newInstance() {
        BodyProcessorChain bodyProcessorChain = new BodyProcessorChain(bodyProcessors());
        return new BodyProcessingFieldTransformer(bodyProcessorChain);
    }
    
    private List<BodyProcessor> bodyProcessors() {
        return asList(        		
        	    stripByAttributesAndValuesBodyProcessor(),        	          
                new RegexRemoverBodyProcessor("(<p[^/>]*>\\s*</p>)|(<p/>)|(<p\\s[^/>]*/>)"),
                new DOMTransformingBodyProcessor(xpathHandlers),
                stAXTransformingBodyProcessor(),
                new RegexRemoverBodyProcessor("(<p>)(\\s|(<br/>))*(</p>)"),
                new RegexReplacerBodyProcessor("</p>(\\r?\\n)+<p>", "</p>" + System.lineSeparator() + "<p>"),
                new RegexReplacerBodyProcessor("</p> +<p>", "</p><p>"),
                new MethodeLinksBodyProcessor(documentStoreApiClient, documentStoreUri),
                new ModularXsltBodyProcessor(xslts()),
                new Html5SelfClosingTagBodyProcessor()      
        );
    }

    private XsltFile[] xslts() {
        try {
            String related = loadResource("xslt/related.xslt");
            return new XsltFile[] { new XsltFile("related", related) };
        } catch (IOException e) {
            throw new BodyProcessingException(e);
        }
    }

    private String loadResource(String name) throws IOException {
        return Resources.toString(Resources.getResource(this.getClass(), name), Charsets.UTF_8);
    }

    private BodyProcessor stAXTransformingBodyProcessor() {
        return new StAXTransformingBodyProcessor(
            new MethodeBodyTransformationXMLEventHandlerRegistry(videoMatcher, interactiveGraphicsMatcher)
        );
    }

    private BodyProcessor stripByAttributesAndValuesBodyProcessor() {
        return new StAXTransformingBodyProcessor(new StripByPredefinedAttributesAndValuesEventHandlerRegistry());
    }
}
