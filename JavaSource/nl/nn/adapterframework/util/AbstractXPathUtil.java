/*
 * AbstractXPathUtil.java
 * 
 * Created on 12-okt-2007, 11:32:58
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.nn.adapterframework.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.io.InputStream;

import org.w3c.dom.DOMException;
import org.w3c.dom.NodeList;


import nl.nn.adapterframework.core.IbisException;

/**
 *
 * @author m00035f
 */
abstract public class AbstractXPathUtil implements XPathUtil {

    public List parseXpath(String xpathExpression, String resourceName) throws IbisException {
        return parseXpath(xpathExpression, this.getClass().getResourceAsStream(resourceName));
    }

    public String parseXpathToString(String xpathExpression, InputStream in) throws IbisException {
        List result = parseXpath(xpathExpression, in);
        StringBuffer b = new StringBuffer();
        for (Iterator it = result.iterator(); it.hasNext();) {
            String item = (String) it.next();
            b.append(item);
        }
        return b.toString();
    }

    public String parseXpathToString(String xpathExpression, String resourceName) throws IbisException {
        return parseXpathToString(xpathExpression, this.getClass().getResourceAsStream(resourceName));
    }

    protected List makeListFromNodeList(NodeList nodes) throws DOMException {
        List result = new ArrayList(nodes.getLength());
        for (int i = 0; i < nodes.getLength(); ++i) {
            result.add(nodes.item(0).getNodeValue());
        }
        return result;
    }

}
