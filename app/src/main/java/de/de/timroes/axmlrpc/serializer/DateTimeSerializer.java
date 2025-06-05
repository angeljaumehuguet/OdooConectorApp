package de.timroes.axmlrpc.serializer;

import org.w3c.dom.Element;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.timroes.axmlrpc.XMLRPCException;
import de.timroes.axmlrpc.XMLUtil;
import de.timroes.axmlrpc.xmlcreator.XmlElement;

public class DateTimeSerializer implements Serializer {

	public static final String DEFAULT_DATETIME_FORMAT = "yyyyMMdd'T'HHmmss";
	private final SimpleDateFormat dateFormatter;
	private final boolean accepts_null_input;

	public DateTimeSerializer(boolean accepts_null_input) {
		this.accepts_null_input = accepts_null_input;
		this.dateFormatter = new SimpleDateFormat(DEFAULT_DATETIME_FORMAT, Locale.US); // Afegit Locale
	}

	public DateTimeSerializer(boolean accepts_null_input, String datetimeFormat) {
		this.accepts_null_input = accepts_null_input;
		this.dateFormatter = new SimpleDateFormat(datetimeFormat, Locale.US); // Afegit Locale
	}

	public Object deserialize(String dateStr) throws XMLRPCException {
		try {
			SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss", Locale.US);
			return format.parse(dateStr);
		} catch (Exception ex) {
			try {
				SimpleDateFormat altFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
				return altFormat.parse(dateStr);
			} catch (Exception ex2) {
				throw new XMLRPCException("No es pot parsejar la data donada amb els formats coneguts: " + dateStr, ex2);
			}
		}
	}

	@Override
	public Object deserialize(Element content) throws XMLRPCException {
		String text = XMLUtil.getOnlyTextContent(content.getChildNodes());
		return deserialize(text); // Crida a l'altre mètode deserialize
	}

	@Override
	public XmlElement serialize(Object object) {
		return XMLUtil.makeXmlTag(SerializerHandler.TYPE_DATETIME,
				dateFormatter.format((Date)object)); // Cast a Date si és necessari
	}
}
