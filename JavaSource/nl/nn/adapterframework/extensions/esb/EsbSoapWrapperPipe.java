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
package nl.nn.adapterframework.extensions.esb;

import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.soap.SoapWrapperPipe;
import nl.nn.adapterframework.util.AppConstants;

/**
 * Extension to SoapWrapperPipe for separate modes.
 *
 * <p><b>Configuration </b><i>(where deviating from SoapWrapperPipe)</i><b>:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setMode(String) mode}</td><td>either <code>i2t</code> (ifsa2tibco), <code>reg</code> (regular) or <code>bis</code> (Business Integration Services)</td><td>reg</td></tr>
 * <tr><td>{@link #setSoapHeaderSessionKey(String) soapHeaderSessionKey}</td><td>if direction=<code>unwrap</code>: </td><td>soapHeader</td></tr>
 * <tr><td>{@link #setSoapHeaderStyleSheet(String) soapHeaderStyleSheet}</td><td>if direction=<code>wrap</code> and mode=<code>i2t</code>:</td><td>/xml/xsl/esb/soapHeader.xsl</td></tr>
 * <tr><td></td><td>if direction=<code>wrap</code> and mode=<code>reg</code>:</td><td>TODO (for now identical to the "<code>i2t</code>" SOAP Header)</td></tr>
 * <tr><td></td><td>if direction=<code>wrap</code> and mode=<code>bis</code>:</td><td>/xml/xsl/esb/bisSoapHeader.xsl</td></tr>
 * <tr><td>{@link #setSoapBodyStyleSheet(String) soapBodyStyleSheet}</td><td>if direction=<code>wrap</code> and mode=<code>reg</code>:</td><td>/xml/xsl/esb/soapBody.xsl</td></tr>
 * <tr><td></td><td>if direction=<code>wrap</code> and mode=<code>bis</code>:</td><td>/xml/xsl/esb/bisSoapBody.xsl</td></tr>
 * <tr><td>{@link #setAddOutputNamespace(boolean) addOutputNamespace}</td><td>(only used when <code>direction=wrap</code>) when <code>true</code>, <code>outputNamespace</code> is automatically set using the parameters ("http://nn.nl/XSD/$businessDomain/$serviceName/$serviceContext/$serviceContextVersion/$operationName/$operationVersion")</td><td><code>false</code></td></tr>
 * </table></p>
 * <p>
 * <b>/xml/xsl/esb/soapHeader.xsl:</b>
 * <table border="1">
 * <tr><th>element</th><th>level</th><th>value</th></tr>
 * <tr><td>MessageHeader</td><td>0</td><td>&nbsp;</td></tr>
 * <tr><td>&nbsp;</td><td>&nbsp;</td><td>xmlns="http://nn.nl/XSD/Generic/MessageHeader/1"</td></tr>
 * <tr><td>From</td><td>1</td><td>&nbsp;</td></tr>
 * <tr><td>Id</td><td>2</td><td>$fromId</td></tr>
 * <tr><td>To</td><td>1</td><td>&nbsp;</td></tr>
 * <tr><td>Location</td><td>2</td><td>if $messagingLayer='P2P' then<br/>&nbsp;$messagingLayer.$businessDomain.$applicationName.$applicationFunction.$paradigm<br/>else<br/>&nbsp;$messagingLayer.$businessDomain.$serviceLayer.$serviceName.$serviceContext.$serviceContextVersion.$operationName.$operationVersion.$paradigm</td></tr>
 * <tr><td>HeaderFields</td><td>1</td><td>&nbsp;</td></tr>
 * <tr><td>CPAId</td><td>2</td><td>$cpaId</td></tr>
 * <tr><td>ConversationId</td><td>2</td><td>$conversationId</td></tr>
 * <tr><td>CorrelationId</td><td>2</td><td>$correlationId (if empty then skip this element)</td></tr>
 * <tr><td>MessageId</td><td>2</td><td>$messageId</td></tr>
 * <tr><td>Timestamp</td><td>2</td><td>$timestamp</td></tr>
 * <tr><td>Service</td><td>1</td><td>&nbsp;</td></tr>
 * <tr><td>Name</td><td>2</td><td>$serviceName</td></tr>
 * <tr><td>Context</td><td>2</td><td>$serviceContext</td></tr>
 * <tr><td>Action</td><td>2</td><td>&nbsp;</td></tr>
 * <tr><td>Paradigm</td><td>3</td><td>$paradigm</td></tr>
 * <tr><td>Name</td><td>3</td><td>$operationName</td></tr>
 * <tr><td>Version</td><td>3</td><td>$operationVersion</td></tr>
 * </table>
 * <b>Parameters:</b>
 * <table border="1">
 * <tr><th>name</th><th>default</th></tr>
 * <tr><td>businessDomain</td><td>&nbsp;</td></tr>
 * <tr><td>serviceName</td><td>&nbsp;</td></tr>
 * <tr><td>serviceContext</td><td>&nbsp;</td></tr>
 * <tr><td>serviceContextVersion</td><td>1</td></tr>
 * <tr><td>operationName</td><td>&nbsp;</td></tr>
 * <tr><td>operationVersion</td><td>1</td></tr>
 * <tr><td>paradigm</td><td>&nbsp;</td></tr>
 * <tr><td>applicationName</td><td>&nbsp;</td></tr>
 * <tr><td>applicationFunction</td><td>&nbsp;</td></tr>
 * <tr><td>messagingLayer</td><td>ESB</td></tr>
 * <tr><td>serviceLayer</td><td>&nbsp;</td></tr>
 * <tr><td>destination</td><td>if not empty this parameters contains the preceding parameters above as described in 'Location' in the table above</td></tr>
 * <tr><td>fromId</td><td>property 'instance.name'</td></tr>
 * <tr><td>cpaId</td><td>if $paradigm equals 'Response' then copied from the original (received) SOAP Header, else 'n/a'</td></tr>
 * <tr><td>conversationId</td><td>if $paradigm equals 'Response' then copied from the original (received) SOAP Header, else parameter pattern '{hostname}_{uid}'</td></tr>
 * <tr><td>messageId</td><td>parameter pattern '{hostname}_{uid}'</td></tr>
 * <tr><td>correlationId</td><td>if applicable, copied from MessageId in the original (received) SOAP Header</td></tr>
 * <tr><td>timestamp</td><td>parameter pattern '{now,date,yyyy-MM-dd'T'HH:mm:ss}'</td></tr>
 * </table>
 * </p>
 * <p>
 * <b>/xml/xsl/esb/bisSoapHeader.xsl:</b>
 * <table border="1">
 * <tr><th>element</th><th>level</th><th>value</th></tr>
 * <tr><td>MessageHeader</td><td>0</td><td>&nbsp;</td></tr>
 * <tr><td>&nbsp;</td><td>&nbsp;</td><td>xmlns=$namespace</td></tr>
 * <tr><td>From</td><td>1</td><td>&nbsp;</td></tr>
 * <tr><td>Id</td><td>2</td><td>$fromId</td></tr>
 * <tr><td>HeaderFields</td><td>1</td><td>&nbsp;</td></tr>
 * <tr><td>ConversationId</td><td>2</td><td>$conversationId</td></tr>
 * <tr><td>CorrelationId</td><td>2</td><td>$correlationId (if empty then skip this element)</td></tr>
 * <tr><td>MessageId</td><td>2</td><td>$messageId</td></tr>
 * <tr><td>ExternalRefToMessageId</td><td>2</td><td>$externalRefToMessageId (if empty then skip this element)</td></tr>
 * <tr><td>Timestamp</td><td>2</td><td>$timestamp</td></tr>
 * </table>
 * <b>Parameters:</b>
 * <table border="1">
 * <tr><th>name</th><th>default</th></tr>
 * <tr><td>namespace</td><td>"http://www.ing.com/CSP/XSD/General/Message_2"</td></tr>
 * <tr><td>fromId</td><td>property 'instance.name'</td></tr>
 * <tr><td>conversationId</td><td>if $paradigm equals 'Response' or 'Reply' then copied from the original (received) SOAP Header, else parameter pattern '{hostname}_{uid}'</td></tr>
 * <tr><td>messageId</td><td>parameter pattern '{hostname}_{uid}'</td></tr>
 * <tr><td>externalRefToMessageId</td><td>if applicable, copied from MessageId in the original (received) SOAP Header</td></tr>
 * <tr><td>timestamp</td><td>parameter pattern '{now,date,yyyy-MM-dd'T'HH:mm:ss}'</td></tr>
 * </table>
 * </p>
 * <p>
 * <b>/xml/xsl/esb/soapBody.xsl:</b>
 * <table border="1">
 * <tr><th>element</th><th>level</th><th>value</th></tr>
 * <tr><td>[Payload]</td><td>0</td><td>if $errorCode is empty then the complete payload will be copied and if not already existing a Result tag will be added<br/>else only the root tag will be copied</td></tr>
 * <tr><td>Result</td><td>1</td><td>this element will be the last child in the copied root tag (only applicable for $paradigm 'Response'); if $errorCode is empty and a Result tag already exists then skip this element including its child elements</td></tr>
 * <tr><td>&nbsp;</td><td>&nbsp;</td><td>xmlns="http://nn.nl/XSD/Generic/MessageHeader/1"</td></tr>
 * <tr><td>Status</td><td>2</td><td>if $errorCode is empty then 'OK'</br>else 'ERROR'</td></tr>
 * <tr><td>ErrorList</td><td>2</td><td>if $errorCode is empty then skip this element including its child elements</td></tr>
 * <tr><td>Error</td><td>3</td><td>&nbsp;</td></tr>
 * <tr><td>Code</td><td>4</td><td>$errorCode</td></tr>
 * <tr><td>Reason</td><td>4</td><td>if $errorReason is not empty then $errorReason</br>else it will be derived from $errorCode:
 *   <table border="1">
 *   <tr><th>errorCode</th><th>errorText</th></tr>
 *   <tr><td>ERR6002</td><td>Service Interface Request Time Out</td></tr>
 *   <tr><td>ERR6003</td><td>Invalid Request Message</td></tr>
 *   <tr><td>ERR6004</td><td>Invalid Backend system response</td></tr>
 *   <tr><td>ERR6005</td><td>Backend system failure response</td></tr>
 *   <tr><td>ERR6999</td><td>Unspecified Errors</td></tr>
 *  </table>
 * </td></tr>
 * <tr><td>Service</td><td>4</td><td>&nbsp;</td></tr>
 * <tr><td>Name</td><td>5</td><td>$serviceName</td></tr>
 * <tr><td>Context</td><td>5</td><td>$serviceContext</td></tr>
 * <tr><td>Action</td><td>5</td><td>&nbsp;</td></tr>
 * <tr><td>Paradigm</td><td>6</td><td>$paradigm</td></tr>
 * <tr><td>Name</td><td>6</td><td>$operationName</td></tr>
 * <tr><td>Version</td><td>6</td><td>$operationVersion</td></tr>
 * <tr><td>DetailList</td><td>4</td><td>if $errorDetailCode is empty then skip this element including its child elements</td></tr>
 * <tr><td>Detail</td><td>5</td><td>&nbsp;</td></tr>
 * <tr><td>Code</td><td>6</td><td>$errorDetailCode</td></tr>
 * <tr><td>Text</td><td>6</td><td>$errorDetailText (if empty then skip this element)</td></tr>
 * </table>
 * <b>Parameters:</b>
 * <table border="1">
 * <tr><th>name</th><th>default</th></tr>
 * <tr><td>namespace</td><td>"http://www.ing.com/CSP/XSD/General/Message_2"</td></tr>
 * <tr><td>errorCode</td><td>&nbsp;</td></tr>
 * <tr><td>errorReason</td><td>&nbsp;</td></tr>
 * <tr><td>errorDetailCode</td><td>&nbsp;</td></tr>
 * <tr><td>errorDetailText</td><td>&nbsp;</td></tr>
 * <tr><td>serviceName</td><td>&nbsp;</td></tr>
 * <tr><td>serviceContext</td><td>&nbsp;</td></tr>
 * <tr><td>operationName</td><td>&nbsp;</td></tr>
 * <tr><td>operationVersion</td><td>1</td></tr>
 * <tr><td>paradigm</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <p>
 * <b>/xml/xsl/esb/bisSoapBody.xsl:</b>
 * <table border="1">
 * <tr><th>element</th><th>level</th><th>value</th></tr>
 * <tr><td>[Payload]</td><td>0</td><td>if $errorCode is empty then the complete payload will be copied and if not already existing a Result tag will be added<br/>else only the root tag will be copied</td></tr>
 * <tr><td>Result</td><td>1</td><td>this element will be the last child in the copied root tag (only applicable for $paradigm 'Response' and 'Reply'); if $errorCode is empty and a Result tag already exists then skip this element including its child elements</td></tr>
 * <tr><td>&nbsp;</td><td>&nbsp;</td><td>xmlns=$namespace</td></tr>
 * <tr><td>Status</td><td>2</td><td>if $errorCode is empty then 'OK'</br>else 'ERROR'</td></tr>
 * <tr><td>ErrorList</td><td>2</td><td>if $errorCode is empty then skip this element including its child elements</td></tr>
 * <tr><td>Error</td><td>3</td><td>&nbsp;</td></tr>
 * <tr><td>Code</td><td>4</td><td>$errorCode</td></tr>
 * <tr><td>Reason</td><td>4</td><td>if $errorReason is not empty then $errorReason</br>else it will be derived from $errorCode:
 *   <table border="1">
 *   <tr><th>errorCode</th><th>errorText</th></tr>
 *   <tr><td>ERR6002</td><td>Service Interface Request Time Out</td></tr>
 *   <tr><td>ERR6003</td><td>Invalid Request Message</td></tr>
 *   <tr><td>ERR6004</td><td>Invalid Backend system response</td></tr>
 *   <tr><td>ERR6005</td><td>Backend system failure response</td></tr>
 *   <tr><td>ERR6999</td><td>Unspecified Errors</td></tr>
 *  </table>
 * </td></tr>
 * <tr><td>Service</td><td>4</td><td>&nbsp;</td></tr>
 * <tr><td>Name</td><td>5</td><td>$serviceName</td></tr>
 * <tr><td>Context</td><td>5</td><td>$serviceContext</td></tr>
 * <tr><td>Action</td><td>5</td><td>&nbsp;</td></tr>
 * <tr><td>Name</td><td>6</td><td>$operationName</td></tr>
 * <tr><td>Version</td><td>6</td><td>$operationVersion</td></tr>
 * <tr><td>DetailList</td><td>4</td><td>if $errorDetailCode is empty then skip this element including its child elements</td></tr>
 * <tr><td>Detail</td><td>5</td><td>&nbsp;</td></tr>
 * <tr><td>Code</td><td>6</td><td>$errorDetailCode</td></tr>
 * <tr><td>Text</td><td>6</td><td>$errorDetailText (if empty then skip this element)</td></tr>
 * </table>
 * <b>Parameters:</b>
 * <table border="1">
 * <tr><th>name</th><th>default</th></tr>
 * <tr><td>errorCode</td><td>&nbsp;</td></tr>
 * <tr><td>errorReason</td><td>&nbsp;</td></tr>
 * <tr><td>errorDetailCode</td><td>&nbsp;</td></tr>
 * <tr><td>errorDetailText</td><td>&nbsp;</td></tr>
 * <tr><td>serviceName</td><td>&nbsp;</td></tr>
 * <tr><td>serviceContext</td><td>&nbsp;</td></tr>
 * <tr><td>operationName</td><td>&nbsp;</td></tr>
 * <tr><td>operationVersion</td><td>1</td></tr>
 * <tr><td>paradigm</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * @version $Id$
 * @author Peter Leeuwenburgh
 */
public class EsbSoapWrapperPipe extends SoapWrapperPipe {
	protected final static String OUTPUTNAMESPACEBASEURI = "http://nn.nl/XSD";
	protected final static String BUSINESSDOMAIN = "businessDomain";
	protected final static String SERVICENAME = "serviceName";
	protected final static String SERVICECONTEXT = "serviceContext";
	protected final static String SERVICECONTEXTVERSION = "serviceContextVersion";
	protected final static String OPERATIONNAME = "operationName";
	protected final static String OPERATIONVERSION = "operationVersion";
	protected final static String PARADIGM = "paradigm";
	protected final static String APPLICATIONNAME = "applicationName";
	protected final static String APPLICATIONFUNCTION = "applicationFunction";
	protected final static String MESSAGINGLAYER = "messagingLayer";
	protected final static String SERVICELAYER = "serviceLayer";
	protected final static String DESTINATION = "destination";
	protected final static String FROMID= "fromId";
	protected final static String CPAID = "cpaId";
	protected final static String CONVERSATIONID = "conversationId";
	protected final static String MESSAGEID = "messageId";
	protected final static String CORRELATIONID = "correlationId";
	protected final static String EXTERNALREFTOMESSAGEID = "externalRefToMessageId";
	protected final static String TIMESTAMP = "timestamp";



    public static enum Mode  {
        I2T,
        REG,
        BIS
    }

	private final static String SOAPHEADER = "soapHeader";


	private Mode mode = Mode.REG;
	private boolean addOutputNamespace = false;

    @Override
	public void configure() throws ConfigurationException {
		if ("unwrap".equalsIgnoreCase(getDirection())) {
			if (StringUtils.isEmpty(getSoapHeaderSessionKey())) {
				setSoapHeaderSessionKey(SOAPHEADER);
			}
		}
		if ("wrap".equalsIgnoreCase(getDirection())) {
			if (StringUtils.isEmpty(getSoapHeaderStyleSheet())) {
				if (mode == Mode.BIS) {
					setSoapHeaderStyleSheet("/xml/xsl/esb/bisSoapHeader.xsl");
				} else {
					setSoapHeaderStyleSheet("/xml/xsl/esb/soapHeader.xsl");
				}
			}
			if (StringUtils.isEmpty(getSoapBodyStyleSheet())) {
				if (mode == Mode.REG) {
					setSoapBodyStyleSheet("/xml/xsl/esb/soapBody.xsl");
				} else if (mode == Mode.BIS) {
					setSoapBodyStyleSheet("/xml/xsl/esb/bisSoapBody.xsl");
				}
			}
			stripDestination();
			if (isAddOutputNamespace()) {
				String ons = getOutputNamespaceBaseUri() +
						"/" + getBusinessDomain() +
						"/" + getServiceName() +
						"/" + getServiceContext() +
						"/" + getServiceContextVersion() +
						"/" + getOperationName() +
						"/" + getOperationVersion();
				setOutputNamespace(ons);
			}
			addParameters();
		}
		super.configure();
	}

    private String getParameterValue(String key) {
        Parameter p = getParameterList().findParameter(key);
        return p != null ? p.getValue() : "";
    }

    public static String getOutputNamespaceBaseUri() {
        return OUTPUTNAMESPACEBASEURI;
    }

    public String getBusinessDomain() {
        return getParameterValue(BUSINESSDOMAIN);
    }

    public String getServiceName() {
        return getParameterValue(SERVICENAME);
    }

    public String getServiceContext() {
        return getParameterValue(SERVICECONTEXT);
    }

    public String getServiceContextVersion() {
        return getParameterValue(SERVICECONTEXTVERSION);
    }

    public String getOperationName() {
        return getParameterValue(OPERATIONNAME);
    }

    public String getOperationVersion() {
        return getParameterValue(OPERATIONVERSION);
    }

    public String getDestination() {
        Parameter p = getParameterList().findParameter(DESTINATION);
        return p == null ? null : p.getValue();
    }

    public String getMessagingLayer() {
        return getParameterValue(MESSAGINGLAYER);
    }

    public String getServiceLayer() {
        return getParameterValue(SERVICELAYER);
    }

    public String getParadigm() {
        return getParameterValue(PARADIGM);
    }

	private void stripDestination() {
		ParameterList parameterList = getParameterList();
		Parameter p = parameterList.findParameter(DESTINATION);
		if (p!=null) {
			String destination = p.getValue();
			//In case the messaging layer is ESB, the destination syntax is:
			// Destination = [MessagingLayer].[BusinessDomain].[ServiceLayer].[ServiceName].[ServiceContext].[ServiceContextVersion].[OperationName].[OperationVersion].[Paradigm]
			//In case the messaging layer is P2P, the destination syntax is:
			// Destination = [MessagingLayer].[BusinessDomain].[ApplicationName].[ApplicationFunction].[Paradigm]
			boolean p2p = false;
			StringTokenizer st = new StringTokenizer(destination,".");
			int count = 0;
			while (st.hasMoreTokens()) {
				count++;
				String str = st.nextToken();
				p = new Parameter();
				switch (count) {
		        	case 1:
		        		if (str.equals("P2P")) {
		        			p2p = true;
		        		}
		        		p.setName(MESSAGINGLAYER);
		        		break;
		        	case 2:
		        		p.setName(BUSINESSDOMAIN);
		        		break;
		        	case 3:
		        		if (p2p) {
			        		p.setName(APPLICATIONNAME);
		        		} else {
			        		p.setName(SERVICELAYER);
		        		}
		        		break;
		        	case 4:
		        		if (p2p) {
			        		p.setName(APPLICATIONFUNCTION);
		        		} else {
			        		p.setName(SERVICENAME);
		        		}
		        		break;
		        	case 5:
		        		if (p2p) {
			        		p.setName(PARADIGM);
		        		} else {
			        		p.setName(SERVICECONTEXT);
		        		}
		        		break;
		        	case 6:
		        		p.setName(SERVICECONTEXTVERSION);
		        		break;
		        	case 7:
		        		p.setName(OPERATIONNAME);
		        		break;
		        	case 8:
		        		p.setName(OPERATIONVERSION);
		        		break;
		        	case 9:
		        		p.setName(PARADIGM);
		        		break;
		        	default:
				}
				p.setValue(str);
				addParameter(p);
			}
		}
	}

	private void addParameters() {
		ParameterList parameterList = getParameterList();
		Parameter p;
		String paradigm = null;
		p = parameterList.findParameter(PARADIGM);
		if (p!=null) {
			paradigm = p.getValue();
		}
		if (parameterList.findParameter(FROMID)==null) {
			p = new Parameter();
			p.setName(FROMID);
			p.setValue(AppConstants.getInstance().getProperty("instance.name", ""));
			addParameter(p);
		}
		if (mode != Mode.BIS) {
			if (parameterList.findParameter(CPAID)==null) {
				p = new Parameter();
				p.setName(CPAID);
				if (paradigm!=null && paradigm.equals("Response")) {
					p.setSessionKey(SOAPHEADER);
					p.setXpathExpression("MessageHeader/HeaderFields/CPAId");
					p.setRemoveNamespaces(true);
				} else {
					p.setValue("n/a");
				}
				addParameter(p);
			}
		}
		if (parameterList.findParameter(CONVERSATIONID)==null) {
			p = new Parameter();
			p.setName(CONVERSATIONID);
			if (paradigm!=null && (paradigm.equals("Response") || paradigm.equals("Reply"))) {
				p.setSessionKey(SOAPHEADER);
				p.setXpathExpression("MessageHeader/HeaderFields/ConversationId");
				p.setRemoveNamespaces(true);
			} else {
				p.setPattern("{hostname}_{uid}");
			}
			addParameter(p);
		}
		if (parameterList.findParameter(MESSAGEID)==null) {
			p = new Parameter();
			p.setName(MESSAGEID);
			p.setPattern("{hostname}_{uid}");
			addParameter(p);
		}
		if (mode == Mode.BIS) {
			if (parameterList.findParameter(EXTERNALREFTOMESSAGEID)==null) {
				p = new Parameter();
				p.setName(EXTERNALREFTOMESSAGEID);
				p.setSessionKey(SOAPHEADER);
				p.setXpathExpression("MessageHeader/HeaderFields/MessageId");
				p.setRemoveNamespaces(true);
				addParameter(p);
			}
		} else {
			if (parameterList.findParameter(CORRELATIONID)==null) {
				p = new Parameter();
				p.setName(CORRELATIONID);
				p.setSessionKey(SOAPHEADER);
				p.setXpathExpression("MessageHeader/HeaderFields/MessageId");
				p.setRemoveNamespaces(true);
				addParameter(p);
			}
		}
		if (parameterList.findParameter(TIMESTAMP)==null) {
			p = new Parameter();
			p.setName(TIMESTAMP);
			p.setPattern("{now,date,yyyy-MM-dd'T'HH:mm:ss}");
			addParameter(p);
		}
	}
	public void setMode(String string) {
		mode = Mode.valueOf(string.toUpperCase());
	}

	public String getMode() {
		return mode.toString();
	}

	public void setAddOutputNamespace(boolean b) {
		addOutputNamespace = b;
	}

	public boolean isAddOutputNamespace() {
		return addOutputNamespace;
	}

	public static boolean isValidNamespace(String namespace) {
		if (namespace != null
				&& namespace.startsWith(getOutputNamespaceBaseUri())) {
			String[] split = namespace.split("/");
			if (split.length == 10) {
				for (int i = 0; i < split.length; i++) {
					if (i == 1) {
						if (split[i].length() != 0) {
							return false;
						}
					} else if (split[i].length() == 0) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

}
