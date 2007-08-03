/*
 * $Log: XmlValidator.java,v $
 * Revision 1.20.2.1  2007-08-03 09:52:50  europe\L190409
 * copied from head
 *
 * Revision 1.23  2007/07/19 07:30:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * put remark about spaces in javadoc
 *
 * Revision 1.22  2007/07/16 11:34:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fill reason in case of parserError, too
 *
 * Revision 1.21  2007/07/10 08:06:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved javadoc
 *
 * Revision 1.20  2007/02/26 13:17:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed non-xml reason setting
 *
 * Revision 1.19  2007/02/05 15:00:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * extended diagnostic information
 *
 * Revision 1.18  2006/08/24 09:24:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * separated finding wrong root from non-wellformedness;
 * used RootElementFindingHandler in XmlUtils.isWellFormed()
 *
 * Revision 1.17  2006/08/23 14:01:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added root-attribute
 *
 * Revision 1.16  2006/01/05 14:36:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.15  2005/12/29 15:19:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected javadoc
 *
 * Revision 1.14  2005/12/28 08:39:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected javadoc
 *
 * Revision 1.13  2005/10/24 09:21:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * inmproved logging
 *
 * Revision 1.12  2005/10/17 11:37:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made thread-safe
 *
 * Revision 1.11  2005/09/27 11:06:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.10  2005/09/26 11:33:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added parserError forward
 *
 * Revision 1.9  2005/09/20 13:26:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed need for baseResourceURL
 *
 * Revision 1.8  2005/09/05 09:33:21  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed typo in methodname setReasonSessionKey()
 *
 * Revision 1.7  2005/09/05 07:01:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added attribute reasonSessionKey
 *
 * Revision 1.6  2005/08/31 16:36:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * reduced logging
 * added usage note about JDK 1.3 vs JDK 1.4
 *
 * Revision 1.5  2005/08/30 16:04:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * rework based on code of Jaco de Groot
 *
 */
package nl.nn.adapterframework.pipes;


import java.net.URL;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.Variant;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlFindingHandler;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 *<code>Pipe</code> that validates the input message against a XML-Schema.
 *
 * <p><b>Notice:</b> this implementation relies on Xerces and is rather
 * version-sensitive. It relies on the validation features of it. You should test the proper
 * working of this pipe extensively on your deployment platform.</p>
 * <p>The XmlValidator relies on the properties for <code>external-schemaLocation</code> and
 * <code>external-noNamespaceSchemaLocation</code>. In
 * Xerces-J-2.4.0 there came a bug-fix for these features, so a previous version was erroneous.<br/>
 * Xerces-j-2.2.1 included a fix on this, so before this version there were problems too (the features did not work).<br/>
 * Therefore: old versions of
 * Xerses on your container may not be able to set the necessary properties, or
 * accept the properties but not do the actual validation! This functionality should
 * work (it does! with Xerces-J-2.6.0 anyway), but testing is necessary!</p>
 * <p><i>Careful 1: test this on your deployment environment</i></p>
 * <p><i>Careful 2: beware of behaviour differences between different JDKs: JDK 1.4 works much better than JDK 1.3</i></p>
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.XmlValidator</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setDurationThreshold(long) durationThreshold}</td><td>if durationThreshold >=0 and the duration (in milliseconds) of the message processing exceeded the value specified the message is logged informatory</td><td>-1</td></tr>
 * <tr><td>{@link #setGetInputFromSessionKey(String) getInputFromSessionKey}</td><td>when set, input is taken from this session key, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStoreResultInSessionKey(String) storeResultInSessionKey}</td><td>when set, the result is stored under this session key</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSchema(String) schema}</td><td>The filename of the schema on the classpath. See doc on the method. (effectively the same as noNamespaceSchemaLocation)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNoNamespaceSchemaLocation(String) noNamespaceSchemaLocation}</td><td>A URI reference as a hint as to the location of a schema document with no target namespace. See doc on the method.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSchemaLocation(String) schemaLocation}</td><td>Pairs of URI references (one for the namespace name, and one for a hint as to the location of a schema document defining names for that namespace name). See doc on the method.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSchemaSessionKey(String) schemaSessionKey}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFullSchemaChecking(boolean) fullSchemaChecking}</td><td>Perform addional memory intensive checks</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setThrowException(boolean) throwException}</td><td>Should the XmlValidator throw a PipeRunException on a validation error (if not, a forward with name "failure" should be defined.</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setReasonSessionKey(String) reasonSessionKey}</td><td>if set: key of session variable to store reasons of mis-validation in</td><td>none</td></tr>
 * <tr><td>{@link #setXmlReasonSessionKey(String) xmlReasonSessionKey}</td><td>like <code>reasonSessionKey</code> but stores reasons in xml format and more extensive</td><td>none</td></tr>
 * <tr><td>{@link #setRoot(String) root}</td><td>name of the root element</td><td>&nbsp;</td></tr>
 * </table>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified, the value for "success"</td></tr>
 * <tr><td>"parserError"</td><td>a parser exception occurred, probably caused by non-well-formed XML. If not specified, "failure" is used in such a case</td></tr>
 * <tr><td>"illegalRoot"</td><td>if the required root element is not found. If not specified, "failure" is used in such a case</td></tr>
 * <tr><td>"failure"</td><td>if a validation error occurred</td></tr>
 * </table>
 * <br>
 * N.B. noNamespaceSchemaLocation may contain spaces, but not if the schema is stored in a .jar or .zip file on the class path.
 * @version Id
 * @author Johan Verrips IOS / Jaco de Groot (***@dynasol.nl)
 */
public class XmlValidator extends FixedForwardPipe {
	public static final String version="$RCSfile: XmlValidator.java,v $ $Revision: 1.20.2.1 $ $Date: 2007-08-03 09:52:50 $";

    private String schemaLocation = null;
    private String noNamespaceSchemaLocation = null;
	private String schemaSessionKey = null;
    private boolean throwException = false;
    private boolean fullSchemaChecking = false;
	private String reasonSessionKey = null;
	private String xmlReasonSessionKey = null;
	private String root = null;

    public class XmlErrorHandler implements ErrorHandler {
        private boolean errorOccured = false;
        private String reasons;
		private XMLReader parser;
		private XmlBuilder xmlReasons = new XmlBuilder("reasons");


		public XmlErrorHandler(XMLReader parser) {
			this.parser = parser;
		}

		public void addReason(String message, String location) {
			try {
				ContentHandler ch = parser.getContentHandler();
				if (ch!=null && ch instanceof XmlFindingHandler) {
					XmlFindingHandler xfh = (XmlFindingHandler)ch;

					XmlBuilder reason = new XmlBuilder("reason");
					XmlBuilder detail;
					
					detail = new XmlBuilder("message");;
					detail.setCdataValue(message);
					reason.addSubElement(detail);

					detail = new XmlBuilder("elementName");;
					detail.setValue(xfh.getElementName());
					reason.addSubElement(detail);

					detail = new XmlBuilder("xpath");;
					detail.setValue(xfh.getXpath());
					reason.addSubElement(detail);
					
					xmlReasons.addSubElement(reason);	
				}
			} catch (Throwable t) {
				log.error("Exception handling errors",t);
				
				XmlBuilder reason = new XmlBuilder("reason");
				XmlBuilder detail;
					
				detail = new XmlBuilder("message");;
				detail.setCdataValue(t.getMessage());
				reason.addSubElement(detail);

				xmlReasons.addSubElement(reason);	
			}

			if (StringUtils.isNotEmpty(location)) {
				message = location + ": " + message;
			}
			errorOccured = true;
			if (reasons == null) {
				 reasons = message;
			 } else {
				 reasons = reasons + "\n" + message;
			 }
		}
		
		public void addReason(Throwable t) {
			String location=null;
			if (t instanceof SAXParseException) {
				SAXParseException spe = (SAXParseException)t;
				location = "at ("+spe.getLineNumber()+ ","+spe.getColumnNumber()+")";
			}
			addReason(t.getMessage(),location);
		}

		public void warning(SAXParseException exception) {
			addReason(exception);
		}
        public void error(SAXParseException exception) {
        	addReason(exception);
        }
        public void fatalError(SAXParseException exception) {
			addReason(exception);
        }

        public boolean hasErrorOccured() {
            return errorOccured;
        }

         public String getReasons() {
            return reasons;
        }

		public String getXmlReasons() {
		   return xmlReasons.toXML();
	   }
    }

    /**
     * Configure the XmlValidator
     * @throws ConfigurationException when:
     * <ul><li>the schema cannot be found</li>
     * <ul><li><{@link #isThrowException()} is false and there is no forward defined
     * for "failure"</li>
     * <li>when the parser does not accept setting the properties for validating</li>
     * </ul>
     */
    public void configure() throws ConfigurationException {
        super.configure();
        
		if (!isThrowException()){
            if (findForward("failure")==null) throw new ConfigurationException(
            getLogPrefix(null)+ "has no forward with name [failure]");
        }
		if ((StringUtils.isNotEmpty(getNoNamespaceSchemaLocation()) ||
			 StringUtils.isNotEmpty(getSchemaLocation())) &&
			StringUtils.isNotEmpty(getSchemaSessionKey())) {
				throw new ConfigurationException(getLogPrefix(null)+"cannot have schemaSessionKey together with schemaLocation or noNamespaceSchemaLocation");
		}
        if (StringUtils.isNotEmpty(getSchemaLocation())) {
        	String resolvedLocations = XmlUtils.resolveSchemaLocations(getSchemaLocation());
        	log.info(getLogPrefix(null)+"resolved schemaLocation ["+getSchemaLocation()+"] to ["+resolvedLocations+"]");
        	setSchemaLocation(resolvedLocations);
        }
		if (StringUtils.isNotEmpty(getNoNamespaceSchemaLocation())) {
			URL url = ClassUtils.getResourceURL(this, getNoNamespaceSchemaLocation());
			if (url==null) {
				throw new ConfigurationException(getLogPrefix(null)+"could not find schema at ["+getNoNamespaceSchemaLocation()+"]");
			}
			String resolvedLocation =url.toExternalForm();
			log.info(getLogPrefix(null)+"resolved noNamespaceSchemaLocation to ["+resolvedLocation+"]");
			setNoNamespaceSchemaLocation(resolvedLocation);
		}
		if (StringUtils.isEmpty(getNoNamespaceSchemaLocation()) &&
			StringUtils.isEmpty(getSchemaLocation()) &&
			StringUtils.isEmpty(getSchemaSessionKey())) {
				throw new ConfigurationException(getLogPrefix(null)+"must have either schemaSessionKey, schemaLocation or noNamespaceSchemaLocation");
		}
    }

	protected PipeRunResult handleFailures(XmlErrorHandler xeh, String input, PipeLineSession session, String mainReason, String forwardName, Throwable t) throws PipeRunException {
		
		String fullReasons=mainReason;
		if (StringUtils.isNotEmpty(xeh.getReasons())) {
			if (StringUtils.isNotEmpty(mainReason)) {
				fullReasons+=":\n"+xeh.getReasons();
			} else {
				fullReasons=xeh.getReasons();
			}
		}
		if (isThrowException()) {
			throw new PipeRunException(this, fullReasons, t);
		} else {
			log.warn(fullReasons, t);
			if (StringUtils.isNotEmpty(getReasonSessionKey())) {
				log.debug(getLogPrefix(session) + "storing reasons under sessionKey ["+getReasonSessionKey()+"]");
				session.put(getReasonSessionKey(),fullReasons);
			}
			if (StringUtils.isNotEmpty(getXmlReasonSessionKey())) {
				log.debug(getLogPrefix(session) + "storing reasons (in xml format) under sessionKey ["+getXmlReasonSessionKey()+"]");
				session.put(getXmlReasonSessionKey(),xeh.getXmlReasons());
			}
				
			PipeForward forward = findForward(forwardName);
			if (forward==null) {
				forward = findForward("failure");
			}
			if (forward==null) {
				throw new PipeRunException(this, fullReasons);
			}
			return new PipeRunResult(forward, input);
		}
	}

     /**
      * Validate the XML string
      * @param input a String
      * @param session a {@link nl.nn.adapterframework.core.PipeLineSession Pipelinesession}

      * @throws PipeRunException when <code>isThrowException</code> is true and a validationerror occurred.
      */
    public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {

        Variant in = new Variant(input);

		if (StringUtils.isNotEmpty(getReasonSessionKey())) {
			log.debug(getLogPrefix(session)+ "removing contents of sessionKey ["+getReasonSessionKey()+ "]");
			session.remove(getReasonSessionKey());
		}

		if (StringUtils.isNotEmpty(getXmlReasonSessionKey())) {
			log.debug(getLogPrefix(session)+ "removing contents of sessionKey ["+getXmlReasonSessionKey()+ "]");
			session.remove(getXmlReasonSessionKey());
		}

		String schemaLocation = getSchemaLocation();
		String noNamespaceSchemaLocation = getNoNamespaceSchemaLocation();

        // Do filename to URL translation if schemaLocation and
        // noNamespaceSchemaLocation are not set. 
        if (schemaLocation == null && noNamespaceSchemaLocation == null) {
   			// now look for the new session way
   			String schemaToBeUsed = getSchemaSessionKey();
   			if (session.containsKey(schemaToBeUsed)) {
				noNamespaceSchemaLocation = session.get(schemaToBeUsed).toString();
   			} else {
   				throw new PipeRunException(this, getLogPrefix(session)+ "cannot retrieve xsd from session variable [" + getSchemaSessionKey() + "]");
    		}
    
    		URL url = ClassUtils.getResourceURL(this, noNamespaceSchemaLocation);
    		if (url == null) {
    			throw new PipeRunException(this, getLogPrefix(session)+ "cannot retrieve [" + noNamespaceSchemaLocation + "]");
    		}
    
			noNamespaceSchemaLocation = url.toExternalForm();
        }

		XmlErrorHandler xeh;
		XMLReader parser=null;
		try {
			parser=getParser(schemaLocation,noNamespaceSchemaLocation);
			if (parser==null) {
				throw new PipeRunException(this, getLogPrefix(session)+ "could not obtain parser");
			}
			xeh = new XmlErrorHandler(parser);
			parser.setErrorHandler(xeh);
		} catch (SAXNotRecognizedException e) {
			throw new PipeRunException(this, getLogPrefix(session)+ "parser does not recognize necessary feature", e);
		} catch (SAXNotSupportedException e) {
			throw new PipeRunException(this, getLogPrefix(session)+ "parser does not support necessary feature", e);
		} catch (SAXException e) {
			throw new PipeRunException(this, getLogPrefix(session)+ "error configuring the parser", e);
		}

		InputSource is = in.asXmlInputSource();

        try {
            parser.parse(is);
         } catch (Exception e) {
			return handleFailures(xeh,(String)input,session,"", "parserError",e);
        }

		boolean illegalRoot = StringUtils.isNotEmpty(getRoot()) && 
							!((XmlFindingHandler)parser.getContentHandler()).getRootElementName().equals(getRoot());
		if (illegalRoot) {
			String str = "got xml with root element '"+((XmlFindingHandler)parser.getContentHandler()).getRootElementName()+"' instead of '"+getRoot()+"'";
			xeh.addReason(str,"");
			return handleFailures(xeh,(String)input,session,"","illegalRoot",null);
		} 
		boolean isValid = !(xeh.hasErrorOccured());
		
		if (!isValid) { 
			String mainReason = getLogPrefix(session) + "got invalid xml according to schema [" + Misc.concatStrings(schemaLocation," ",noNamespaceSchemaLocation) + "]";
			return handleFailures(xeh,(String)input,session,mainReason,"failure",null);
        }
        return new PipeRunResult(getForward(), input);
    }


    /**
     * Get a configured parser.
     */
    private XMLReader getParser(String schemaLocation, String noNamespaceSchemaLocation) throws SAXNotRecognizedException, SAXNotSupportedException, SAXException {
        XMLReader parser = null;
        parser = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
        parser.setFeature("http://xml.org/sax/features/validation", true);
        parser.setFeature("http://xml.org/sax/features/namespaces", true);
        parser.setFeature("http://apache.org/xml/features/validation/schema", true);
        if (schemaLocation != null) {
            log.debug("Give schemaLocation to parser: " + schemaLocation);
            parser.setProperty("http://apache.org/xml/properties/schema/external-schemaLocation", schemaLocation);
        }
        if (noNamespaceSchemaLocation != null) {
			log.debug("Give noNamespaceSchemaLocation to parser: " + noNamespaceSchemaLocation);
            parser.setProperty("http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation", noNamespaceSchemaLocation);
        }
        if (isFullSchemaChecking()) {
            parser.setFeature("http://apache.org/xml/features/validation/schema-full-checking", true);
        }
        if (StringUtils.isNotEmpty(getRoot()) || StringUtils.isNotEmpty(getXmlReasonSessionKey())) {    
        	parser.setContentHandler(new XmlFindingHandler());
        }
        return parser;
    }
    
    /**
     * Enable full schema grammar constraint checking, including
     * checking which may be time-consuming or memory intensive.
     *  Currently, particle unique attribution constraint checking and particle
     * derivation resriction checking are controlled by this option.
     * <p> see property http://apache.org/xml/features/validation/schema-full-checking</p>
     * Defaults to <code>false</code>;
     */
    public void setFullSchemaChecking(boolean fullSchemaChecking) {
        this.fullSchemaChecking = fullSchemaChecking;
    }
	public boolean isFullSchemaChecking() {
		return fullSchemaChecking;
	}

    /**
     * <p>The filename of the schema on the classpath. The filename (which e.g.
     * can contain spaces) is translated to an URI with the
     * ClassUtils.getResourceURL(Object,String) method (e.g. spaces are translated to %20).
     * It is not possible to specify a namespace using this attribute.
     * <p>An example value would be "xml/xsd/GetPartyDetail.xsd"</p>
     * <p>The value of the schema attribute is only used if the schemaLocation
     * attribute and the noNamespaceSchemaLocation are not set</p>
     * @see ClassUtils.getResource(Object,String)
     */
    public void setSchema(String schema) {
        setNoNamespaceSchemaLocation(schema);
    }
	public String getSchema() {
		return getNoNamespaceSchemaLocation();
	}

    /**
     * <p>Pairs of URI references (one for the namespace name, and one for a
     * hint as to the location of a schema document defining names for that
     * namespace name).</p>
     * <p> The syntax is the same as for schemaLocation attributes
     * in instance documents: e.g, "http://www.example.com file%20name.xsd".</p>
     * <p>The user can specify more than one XML Schema in the list.</p>
     * <p><b>Note</b> that spaces are considered separators for this attributed. 
     * This means that, for example, spaces in filenames should be escaped to %20.
     * </p>
     * 
     * N.B. since 4.3.0 schema locations are resolved automatically, without the need for ${baseResourceURL}
     */
    public void setSchemaLocation(String schemaLocation) {
        this.schemaLocation = schemaLocation;
    }
	public String getSchemaLocation() {
		return schemaLocation;
	}

    /**
     * <p>A URI reference as a hint as to the location of a schema document with
     * no target namespace.</p>
     */
    public void setNoNamespaceSchemaLocation(String noNamespaceSchemaLocation) {
        this.noNamespaceSchemaLocation = noNamespaceSchemaLocation;
    }
	public String getNoNamespaceSchemaLocation() {
		return noNamespaceSchemaLocation;
	}

	/**
	 * <p>The sessionkey to a value that is the uri to the schema definition.</P>
	 */
	public void setSchemaSessionKey(String schemaSessionKey) {
		this.schemaSessionKey = schemaSessionKey;
	}
	public String getSchemaSessionKey() {
		return schemaSessionKey;
	}

	/**
	 * @deprecated attribute name changed to {@link #setSchemaSessionKey(String) schemaSessionKey}
	 */
	public void setSchemaSession(String schemaSessionKey) {
		log.warn(getLogPrefix(null)+"attribute 'schemaSession' is deprecated. Please use 'schemaSessionKey' instead.");
		this.schemaSessionKey = schemaSessionKey;
	}


    /**
     * Indicates wether to throw an error (piperunexception) when
     * the xml is not compliant.
     */
    public void setThrowException(boolean throwException) {
        this.throwException = throwException;
    }
	public boolean isThrowException() {
		return throwException;
	}
	
	/**
	 * The sessionkey to store the reasons of misvalidation in.
	 */
	public void setReasonSessionKey(String reasonSessionKey) {
		this.reasonSessionKey = reasonSessionKey;
	}
	public String getReasonSessionKey() {
		return reasonSessionKey;
	}

	public void setXmlReasonSessionKey(String xmlReasonSessionKey) {
		this.xmlReasonSessionKey = xmlReasonSessionKey;
	}
	public String getXmlReasonSessionKey() {
		return xmlReasonSessionKey;
	}

	public void setRoot(String root) {
		this.root = root;
	}
	public String getRoot() {
		return root;
	}

}
