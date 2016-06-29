package com.ft.methodearticletransformer.transformation;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.ft.bodyprocessing.xml.dom.XPathHandler;
import com.ft.methodearticletransformer.model.concordance.Concordances;
import com.ft.methodearticletransformer.model.concordance.Concordance;
import com.sun.jersey.api.client.Client;


public class TearSheetLinksTransformer implements XPathHandler {
	private static final Logger LOG = LoggerFactory.getLogger(TearSheetLinksTransformer.class);

	private final static String TME_AUTHORITY="http://api.ft.com/system/FT-TME";
	private final static String CONCEPT_TAG="ft-concept";
	private final static String COMPANY_TYPE= "http://www.ft.com/ontology/company/Company";
	private final Client client;
	private final URI concordanceAPI;
	
	public TearSheetLinksTransformer(Client client, URI concordanceAPI) {
		this.client = client;
		this.concordanceAPI = concordanceAPI;
	}

	@Override
	public void handle(Document document, NodeList nodes) {
		int len = nodes.getLength();
		if (len > 0) {
			UriBuilder builder = UriBuilder.fromUri(concordanceAPI);
			builder.queryParam("authority", TME_AUTHORITY);

			for (int i = len - 1; i >= 0; i--) {
				Element el = (Element) nodes.item(i);
				String id = el.getAttribute("CompositeId");
				if (StringUtils.isNotBlank(id)) {
					builder.queryParam("identifierValue", id);
				}
				System.out.println(id + " " + i);
			}
			URI concordanceApiQuery = builder.build();
			Concordances response = client.resource(concordanceApiQuery).get(Concordances.class);
			replaceLinkToContentPresentInDocumentStore(response.getConcordances(), nodes);
		}
	}
	
	private void replaceLinkToContentPresentInDocumentStore(List<Concordance> concordances, NodeList nodes) {
		int len = nodes.getLength();
		for (int i = len - 1; i >= 0; i--) {
			Element el = (Element) nodes.item(i);
			String id = el.getAttribute("CompositeId");
			if (StringUtils.isNotBlank(id)) {
				String conceptApiUrl = getConcordanceByTMEId(concordances, id);
				if (StringUtils.isNotBlank(conceptApiUrl)) {
					Element newElement = el.getOwnerDocument().createElement(CONCEPT_TAG);
					newElement.setAttribute("url", conceptApiUrl);
					newElement.setAttribute("type", COMPANY_TYPE);
					newElement.setTextContent(el.getTextContent());
					el.getParentNode().replaceChild(newElement, el);
				} else {
					LOG.warn("Composite Id is not concorded  id=" + id);
				}
			}
		}
	}
	
	private String getConcordanceByTMEId(List<Concordance> concordances, String TMEId) {

		Optional<Concordance> concordance = concordances.stream()
				.filter(item -> item.getIdentifier().getAuthority().equals(TME_AUTHORITY)
						&& TMEId.equals(item.getIdentifier().getIdentifierValue()))
				.findFirst();

		if (concordance.isPresent()) {
			return concordance.get().getConcept().getApiUrl();
		}

		return null;
	}
}
