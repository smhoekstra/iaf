/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.util;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.SoftReferenceObjectPool;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

/**
 * Pool of transformers. As of IBIS 4.2.e the Templates object is used to improve
 * performance and work around threading problems with the api.  
 * 
 * @author Gerrit van Brakel
 */
public class TransformerPool {
	public static final String version = "$RCSfile: TransformerPool.java,v $ $Revision: 1.26 $ $Date: 2012-08-10 11:29:11 $";
	protected Logger log = LogUtil.getLogger(this);

	private TransformerFactory tFactory;

	private Templates templates;
	private URL reloadURL=null;
	
	private ObjectPool pool = new SoftReferenceObjectPool(new BasePoolableObjectFactory() {
		public Object makeObject() throws Exception {
			return createTransformer();			
		}
	}); 

	public TransformerPool(Source source, String sysId) throws TransformerConfigurationException {
		this(source,sysId,false);
	}	

	public TransformerPool(Source source, String sysId, boolean xslt2) throws TransformerConfigurationException {
		super();
		tFactory = XmlUtils.getTransformerFactory(xslt2);
		initTransformerPool(source, sysId);

		// check if a transformer can be initiated
		Transformer t = getTransformer();
		
		releaseTransformer(t);
	}	
	
	public TransformerPool(Source source) throws TransformerConfigurationException {
		this(source,false);
	}	

	public TransformerPool(Source source, boolean xslt2) throws TransformerConfigurationException {
		this(source,null,xslt2);
	}	

	public TransformerPool(URL url) throws TransformerConfigurationException, IOException {
		this(url, false);
	}

	public TransformerPool(URL url, boolean xslt2) throws TransformerConfigurationException, IOException {
		this(new StreamSource(url.openStream(),Misc.DEFAULT_INPUT_STREAM_ENCODING),url.toString(),xslt2);
	}
	
	public TransformerPool(String xsltString) throws TransformerConfigurationException {
		this(xsltString, false);
	}

	public TransformerPool(String xsltString, boolean xslt2) throws TransformerConfigurationException {
		this(new StreamSource(new StringReader(xsltString)),xslt2);
	}

	public TransformerPool(String xsltString, String sysId) throws TransformerConfigurationException {
		this(xsltString, sysId, false);
	}
	public TransformerPool(String xsltString, String sysId, boolean xslt2) throws TransformerConfigurationException {
		this(new StreamSource(new StringReader(xsltString)), sysId, xslt2);
	}

	private void initTransformerPool(Source source, String sysId) throws TransformerConfigurationException {
		if (StringUtils.isNotEmpty(sysId)) {
			source.setSystemId(sysId);
			log.debug("setting systemId to ["+sysId+"]");
		}
		templates=tFactory.newTemplates(source);
	}

	private void reloadTransformerPool() throws TransformerConfigurationException, IOException {
		if (reloadURL!=null) {
			initTransformerPool(new StreamSource(reloadURL.openStream(),Misc.DEFAULT_INPUT_STREAM_ENCODING),reloadURL.toString());
			try {
				pool.clear();
			} catch (Exception e) {
				throw new TransformerConfigurationException("Could not clear pool",e);
			}
		}
	}

	public static TransformerPool configureTransformer(String logPrefix, String namespaceDefs, String xPathExpression, String styleSheetName, String outputType, boolean includeXmlDeclaration, ParameterList params, boolean mandatory) throws ConfigurationException {
		if (mandatory || StringUtils.isNotEmpty(xPathExpression) || StringUtils.isNotEmpty(styleSheetName)) {
			return configureTransformer(logPrefix,namespaceDefs,xPathExpression,styleSheetName, outputType, includeXmlDeclaration, params);
		} 
		return null;
	}
	
	public static TransformerPool configureTransformer(String logPrefix, String namespaceDefs, String xPathExpression, String styleSheetName, String outputType, boolean includeXmlDeclaration, ParameterList params) throws ConfigurationException {
		return configureTransformer0(logPrefix,namespaceDefs,xPathExpression,styleSheetName,outputType,includeXmlDeclaration,params,false);
	}

	public static TransformerPool configureTransformer0(String logPrefix, String namespaceDefs, String xPathExpression, String styleSheetName, String outputType, boolean includeXmlDeclaration, ParameterList params, boolean xslt2) throws ConfigurationException {
		TransformerPool result;
		if (logPrefix==null) {
			logPrefix="";
		}
		if (StringUtils.isNotEmpty(xPathExpression)) {
			if (StringUtils.isNotEmpty(styleSheetName)) {
				throw new ConfigurationException(logPrefix+" cannot have both an xpathExpression and a styleSheetName specified");
			}
			try {
				List paramNames = null;
				if (params!=null) {
					paramNames = new ArrayList();
					Iterator iterator = params.iterator();
					while (iterator.hasNext()) {
						paramNames.add(((Parameter)iterator.next()).getName());
					}
				}
				result = new TransformerPool(XmlUtils.createXPathEvaluatorSource(namespaceDefs,xPathExpression, outputType, includeXmlDeclaration, paramNames), xslt2);
			} 
			catch (TransformerConfigurationException te) {
				throw new ConfigurationException(logPrefix+" got error creating transformer from xpathExpression [" + xPathExpression + "] namespaceDefs [" + namespaceDefs + "]", te);
			}
		} 
		else {
			if (StringUtils.isNotEmpty(namespaceDefs)) {
				throw new ConfigurationException(logPrefix+" cannot have namespaceDefs specified for a styleSheetName");
			}
			if (!StringUtils.isEmpty(styleSheetName)) {
				URL resource = ClassUtils.getResourceURL(TransformerPool.class, styleSheetName);
				if (resource==null) {
					throw new ConfigurationException(logPrefix+" cannot find ["+ styleSheetName + "]"); 
				}
				try {
					result = new TransformerPool(resource, xslt2);
				} catch (IOException e) {
					throw new ConfigurationException(logPrefix+"cannot retrieve ["+ styleSheetName + "], resource ["+resource.toString()+"]", e);
				} catch (TransformerConfigurationException te) {
					throw new ConfigurationException(logPrefix+" got error creating transformer from file [" + styleSheetName + "]", te);
				}
				if (XmlUtils.isAutoReload()) {
					result.reloadURL=resource;
				}
			} else {
				throw new ConfigurationException(logPrefix+" either xpathExpression or styleSheetName must be specified");
			}
		}
		return result;
	}
	

	
	public void open() throws Exception {
	}
	
	public void close() {
		try {
			pool.clear();			
		} catch (Exception e) {
			log.warn("exception clearing transformerPool",e);
		}
	}
	
	protected Transformer getTransformer() throws TransformerConfigurationException {
		try {
			reloadTransformerPool();
			return (Transformer)pool.borrowObject();
		} catch (Exception e) {
			throw new TransformerConfigurationException(e);
		}
	}
	
	protected void releaseTransformer(Transformer t) throws TransformerConfigurationException {
		try {
			pool.returnObject(t);
		} catch (Exception e) {
			throw new TransformerConfigurationException("exception returning transformer to pool", e);
		}
	}

	protected void invalidateTransformer(Transformer t) throws Exception {
		pool.invalidateObject(t);
	}

	protected void invalidateTransformerNoThrow(Transformer transformer) {
		try {
			invalidateTransformer(transformer);
			log.debug("Transformer was removed from pool as an error occured on the last transformation");
		} catch (Throwable t) {
			log.error("Error on removing transformer from pool", t);
		}
	}


	protected synchronized Transformer createTransformer() throws TransformerConfigurationException {
		Transformer t = templates.newTransformer();
		if (t==null) {
			throw new TransformerConfigurationException("cannot instantiate transformer");
		}
		t.setErrorListener(new TransformerErrorListener());
		return t;
	}

	public String transform(Document d, Map parameters)	throws TransformerException, IOException {
		return transform(new DOMSource(d),parameters);
	}

	public String transform(String s, Map parameters) throws TransformerException, IOException, DomBuilderException {
		return transform(XmlUtils.stringToSourceForSingleUse(s),parameters);
	}

	public String transform(String s, Map parameters, boolean namespaceAware) throws TransformerException, IOException, DomBuilderException {
		return transform(XmlUtils.stringToSourceForSingleUse(s, namespaceAware),parameters);
	}

	public String transform(Source s, Map parameters) throws TransformerException, IOException {
		return transform(s, null, parameters);
	}

	public String transform(Source s, Result r, Map parameters) throws TransformerException, IOException {
		Transformer transformer = getTransformer();
		try {
			if (r == null) {
				XmlUtils.setTransformerParameters(transformer, parameters);
				return XmlUtils.transformXml(transformer, s);
			} else {
				XmlUtils.setTransformerParameters(transformer, parameters);
				transformer.transform(s,r);
			}
		} catch (TransformerException te) {
			((TransformerErrorListener)transformer.getErrorListener()).setFatalTransformerException(te);
		} catch (IOException ioe) {
			((TransformerErrorListener)transformer.getErrorListener()).setFatalIOException(ioe);
		} 
		finally {
			if (transformer != null) {
				TransformerErrorListener transformerErrorListener = (TransformerErrorListener)transformer.getErrorListener();
				if (transformerErrorListener.getFatalTransformerException() != null) {
					invalidateTransformerNoThrow(transformer);
					throw transformerErrorListener.getFatalTransformerException();
				}
				if (transformerErrorListener.getFatalIOException() != null) {
					invalidateTransformerNoThrow(transformer);
					throw transformerErrorListener.getFatalIOException();
				}
				try {
					releaseTransformer(transformer);
				} catch(Exception e) {
					log.warn("Exception returning transformer to pool",e);
				};
			}
		}
		return null;
	}

	private class TransformerErrorListener implements ErrorListener {
		private TransformerException fatalTransformerException;
		private IOException fatalIOException;

		TransformerErrorListener() {
		}

		public void error(TransformerException transformerException)
				throws TransformerException {
			log.warn("Nonfatal transformation error: " + transformerException.getMessage());
		}

		public void fatalError(TransformerException transformerException)
				throws TransformerException {
			this.setFatalTransformerException(transformerException);
		}

		public void warning(TransformerException transformerException)
				throws TransformerException {
			log.warn("Nonfatal transformation warning: " + transformerException.getMessage());
		}

		public void setFatalTransformerException(
				TransformerException fatalTransformerException) {
			this.fatalTransformerException = fatalTransformerException;
		}

		public TransformerException getFatalTransformerException() {
			return fatalTransformerException;
		}

		public void setFatalIOException(IOException fatalIOException) {
			this.fatalIOException = fatalIOException;
		}

		public IOException getFatalIOException() {
			return fatalIOException;
		}
	}

}
