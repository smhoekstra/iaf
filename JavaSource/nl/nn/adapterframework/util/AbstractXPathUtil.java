/*
 * AbstractXPathUtil.java
 * 
 * Created on 12-okt-2007, 11:32:58
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.nn.adapterframework.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.io.InputStream;

import org.w3c.dom.DOMException;
import org.w3c.dom.NodeList;


import nl.nn.adapterframework.core.IbisException;
import org.w3c.dom.NamedNodeMap;

/**
 *
 * @author m00035f
 */
abstract public class AbstractXPathUtil implements XPathUtil {

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.util.XPathUtil#parseXpathToString(java.lang.String, java.lang.String)
     */
    public String parseXpathToString(String xpathExpression, String resourceName) throws IbisException {
        InputStream in = null;
        try {
            in = ClassUtils.getResourceURL(this, resourceName).openStream();
            return parseXpathToString(xpathExpression, in);
        } catch (IOException ex) {
            throw new IbisException(ex);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Convert a list to a String
     * 
     * Appends all items from the list without any spaces or breaks
     * @param result
     * @return
     */
    protected String listToString(List result) {
        StringBuffer b = new StringBuffer();
        for (Iterator it = result.iterator(); it.hasNext();) {
            String item = (String) it.next();
            b.append(item);
        }
        return b.toString();
    }

    /**
     * Make a list of String from a DOM NodeList contains Node elements.
     * @param nodes
     * @return
     * @throws DOMException
     */
    protected List makeListFromNodeList(NodeList nodes) throws DOMException {
        List result = new ArrayList(nodes.getLength());
        for (int i = 0; i < nodes.getLength(); ++i) {
            result.add(nodes.item(0).getNodeValue());
        }
        return result;
    }

    /**
     * Make a list of String from a DOM NodeList contains Node elements.
     * @param nodes
     * @return
     * @throws DOMException
     */
    protected List makeListFromNodeMap(NamedNodeMap nodes) throws DOMException {
        List result = new ArrayList(nodes.getLength());
        for (int i = 0; i < nodes.getLength(); ++i) {
            result.add(nodes.item(0).getNodeValue());
        }
        return result;
    }
}
