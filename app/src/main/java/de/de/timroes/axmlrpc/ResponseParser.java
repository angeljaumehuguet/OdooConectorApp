package de.timroes.axmlrpc;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import de.timroes.axmlrpc.serializer.SerializerHandler;

public class ResponseParser {

	private static final String FAULT_CODE = "faultCode";
	private static final String FAULT_STRING = "faultString";

	@SuppressWarnings("unchecked")
	public Object parse(SerializerHandler serializerHandler, InputStream response, boolean debugMode) throws XMLRPCException {

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			factory.setIgnoringComments(true);
			factory.setIgnoringElementContentWhitespace(true);

			try {
				factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
				factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
				factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
				factory.setExpandEntityReferences(false);
			} catch (ParserConfigurationException e) {
				Log.w("ResponseParser", "No s'han pogut establir algunes característiques de seguretat del parser XML.", e);
			}

			DocumentBuilder builder = factory.newDocumentBuilder();
			Document dom = builder.parse(response);

			if (debugMode) {
				try {
					printDocument(dom, System.out);
				} catch (Exception ex) {
					Log.e("ResponseParser", "Error imprimint el document de depuració.", ex);
				}
			}

			Element e = dom.getDocumentElement();

			if (!e.getNodeName().equals(XMLRPCClient.METHOD_RESPONSE)) {
				throw new XMLRPCException("MethodResponse root tag is missing.");
			}

			e = XMLUtil.getOnlyChildElement(e.getChildNodes());

			if (e.getNodeName().equals(XMLRPCClient.PARAMS)) {
				e = XMLUtil.getOnlyChildElement(e.getChildNodes());
				if (!e.getNodeName().equals(XMLRPCClient.PARAM)) {
					throw new XMLRPCException("The params tag must contain a param tag.");
				}
				return getReturnValueFromElement(serializerHandler, e);
			} else if (e.getNodeName().equals(XMLRPCClient.FAULT)) {
				Map<String, Object> o = (Map<String, Object>) getReturnValueFromElement(serializerHandler, e);
				throw new XMLRPCServerException((String) o.get(FAULT_STRING), (Integer) o.get(FAULT_CODE));
			}

			throw new XMLRPCException("The methodResponse tag must contain a fault or params tag.");

		} catch (XMLRPCServerException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new XMLRPCException("Error getting result from server.", ex);
		}
	}

	public static void printDocument(Document doc, OutputStream out) throws IOException, TransformerException {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

		transformer.transform(new DOMSource(doc),
				new StreamResult(new OutputStreamWriter(out, "UTF-8")));
	}

	private Object getReturnValueFromElement(SerializerHandler serializerHandler, Element element) throws XMLRPCException {
		Element childElement = XMLUtil.getOnlyChildElement(element.getChildNodes());
		return serializerHandler.deserialize(childElement);
	}
}