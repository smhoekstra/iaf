/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.nn.adapterframework.util;

import java.io.InputStream;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import nl.nn.adapterframework.core.IbisException;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 *
 * @author Tim
 */
public class XPathUtilXslt extends AbstractXPathUtil implements XPathUtil {
    private final static Logger log = LogUtil.getLogger(XPathUtilXslt.class);
    
    public String parseXpathToString(String xpathExpression, InputStream in) throws IbisException {
        // NB: It's not strictly neccesary to override this method as the
        // default implementation would have done just fine, but I expect
        // this to be a more efficient implementation.
        try {
            Source s = new StreamSource(in);
            Transformer t = XmlUtils.createXPathEvaluator(xpathExpression);
            return XmlUtils.transformXml(t, s);
        } catch (Exception ex) {
            throw new IbisException("Failed to parse XML against XPath expression '"
                    + xpathExpression + "'", ex);
        }
    }
    
    
}
