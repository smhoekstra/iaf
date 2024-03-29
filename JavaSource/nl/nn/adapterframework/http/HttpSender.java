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
package nl.nn.adapterframework.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.Misc;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.lang.StringUtils;

/**
 * Sender that gets information via a HTTP using POST or GET.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.http.HttpSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUrl(String) url}</td><td>URL or base of URL to be used </td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUrlParam(String) urlParam}</td><td>parameter that is used to obtain url; overrides url-attribute.</td><td>url</td></tr>
 * <tr><td>{@link #setMethodType(String) methodType}</td><td>type of method to be executed, either 'GET' or 'POST'</td><td>GET</td></tr>
 * <tr><td>{@link #setContentType(String) contentType}</td><td>conent-type of the request, only for POST methods</td><td>text/html; charset=UTF-8</td></tr>
 * <tr><td>{@link #setTimeout(int) timeout}</td><td>timeout in ms of obtaining a connection/result. 0 means no timeout</td><td>10000</td></tr>
 * <tr><td>{@link #setMaxConnections(int) maxConnections}</td><td>the maximum number of concurrent connections</td><td>2</td></tr>
 * <tr><td>{@link #setMaxExecuteRetries(int) maxExecuteRetries}</td><td>the maximum number of times it the execution is retried</td><td>1</td></tr>
 * <tr><td>{@link #setAuthAlias(String) authAlias}</td><td>alias used to obtain credentials for authentication to host</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUserName(String) userName}</td><td>username used in authentication to host</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPassword(String) password}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyHost(String) proxyHost}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyPort(int) proxyPort}</td><td>&nbsp;</td><td>80</td></tr>
 * <tr><td>{@link #setProxyAuthAlias(String) proxyAuthAlias}</td><td>alias used to obtain credentials for authentication to proxy</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyUserName(String) proxyUserName}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyPassword(String) proxyPassword}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyRealm(String) proxyRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCertificate(String) certificate}</td><td>resource URL to certificate to be used for authentication</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCertificateAuthAlias(String) certificateAuthAlias}</td><td>alias used to obtain certificate password</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCertificatePassword(String) certificatePassword}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setKeystoreType(String) keystoreType}</td><td>&nbsp;</td><td>pkcs12</td></tr>
 * <tr><td>{@link #setKeyManagerAlgorithm(String) keyManagerAlgorithm}</td><td>&nbsp;</td><td></td></tr>
 * <tr><td>{@link #setTruststore(String) truststore}</td><td>resource URL to truststore to be used for authentication</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTruststoreAuthAlias(String) truststoreAuthAlias}</td><td>alias used to obtain truststore password</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTruststorePassword(String) truststorePassword}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTruststoreType(String) truststoreType}</td><td>&nbsp;</td><td>jks</td></tr>
 * <tr><td>{@link #setTrustManagerAlgorithm(String) trustManagerAlgorithm}</td><td>&nbsp;</td><td></td></tr>
 * <tr><td>{@link #setAllowSelfSignedCertificates(boolean) allowSelfSignedCertificates}</td><td>when true, self signed certificates are accepted</td><td>false</td></tr>
 * <tr><td>{@link #setFollowRedirects(boolean) followRedirects}</td><td>when true, a redirect request will be honoured, e.g. to switch to https</td><td>true</td></tr>
 * <tr><td>{@link #setVerifyHostname(boolean) verifyHostname}</td><td>when true, the hostname in the certificate will be checked against the actual hostname</td><td>true</td></tr>
 * <tr><td>{@link #setJdk13Compatibility(boolean) jdk13Compatibility}</td><td>enables the use of certificates on JDK 1.3.x. The SUN reference implementation JSSE 1.0.3 is included for convenience</td><td>false</td></tr>
 * <tr><td>{@link #setStaleChecking(boolean) staleChecking}</td><td>controls whether connections checked to be stale, i.e. appear open, but are not.</td><td>true</td></tr>
 * <tr><td>{@link #setEncodeMessages(boolean) encodeMessages}</td><td>specifies whether messages will encoded, e.g. spaces will be replaced by '+' etc.</td><td>false</td></tr>
 * <tr><td>{@link #setParamsInUrl(boolean) paramsInUrl}</td><td>when false and <code>methodeType=POST</code>, request parameters are put in the request body instead of in the url</td><td>true</td></tr>
 * <tr><td>{@link #setInputMessageParam(String) inputMessageParam}</td><td>(only used when <code>methodeType=POST</code> and <code>paramsInUrl=false</code>) name of the request parameter which is used to put the input message in</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setIgnoreRedirects(boolean) ignoreRedirects}</td><td>when true, besides http status code 200 (OK) also the code 301 (MOVED_PERMANENTLY), 302 (MOVED_TEMPORARILY) and 307 (TEMPORARY_REDIRECT) are considered successful</td><td>false</td></tr>
 * </table>
 * </p>
 * <p><b>Parameters:</b></p>
 * <p>Any parameters present are appended to the request as request-parameters</p>
 * 
 * <p><b>Expected message format:</b></p>
 * <p>GET methods expect a message looking like this</p>
 * <pre>
 *   param_name=param_value&another_param_name=another_param_value
 * </pre>
 * <p>POST methods expect a message similar as GET, or looking like this</p>
 * <pre>
 *   param_name=param_value
 *   another_param_name=another_param_value
 * </pre>
 *
 * <p>
 * Note 1:
 * Some certificates require the &lt;java_home&gt;/jre/lib/security/xxx_policy.jar files to be upgraded to unlimited strength. Typically, in such a case, an error message like 
 * <code>Error in loading the keystore: Private key decryption error: (java.lang.SecurityException: Unsupported keysize or algorithm parameters</code> is observed.
 * For IBM JDKs these files can be downloaded from http://www.ibm.com/developerworks/java/jdk/security/50/ (scroll down to 'IBM SDK Policy files')
 * </p>
 * Replace in the directory java\jre\lib\security the following files:
 * <ul>
 * <li>local_policy.jar</li>
 * <li>US_export_policy.jar</li>
 * </ul>
 * <p>
 * Note 2:
 * To debug ssl-related problems, set the following system property:
 * <ul>
 * <li>IBM / WebSphere: <code>-Djavax.net.debug=true</code></li>
 * <li>SUN: <code>-Djavax.net.debug=all</code></li>
 * </ul>
 * </p>
 * <p>
 * Note 3:
 * In case <code>javax.net.ssl.SSLHandshakeException: unknown certificate</code>-exceptions are thrown, 
 * probably the certificate of the other party is not trusted. Try to use one of the certificates in the path as your truststore by doing the following:
 * <ul>
 *   <li>open the URL you are trying to reach in InternetExplorer</li>
 *   <li>click on the yellow padlock on the right in the bottom-bar. This opens the certificate information window</li>
 *   <li>click on tab 'Certificeringspad'</li>
 *   <li>double click on root certificate in the tree displayed. This opens the certificate information window for the root certificate</li>
 *   <li>click on tab 'Details'</li>
 *   <li>click on 'Kopieren naar bestand'</li>
 *   <li>click 'next', choose 'DER Encoded Binary X.509 (.CER)'</li>
 *   <li>click 'next', choose a filename</li>
 *   <li>click 'next' and 'finish'</li>
 * 	 <li>Start IBM key management tool ikeyman.bat, located in Program Files/IBM/WebSphere Studio/Application Developer/v5.1.2/runtimes/base_v51/bin (or similar)</li>
 *   <li>create a new key-database (Sleuteldatabase -> Nieuw...), or open the default key.jks (default password="changeit")</li>
 *   <li>add the generated certificate (Toevoegen...)</li>
 *   <li>store the key-database in JKS format</li>
 *   <li>if you didn't use the standard keydatabase, then reference the file in the truststore-attribute in Configuration.xml (include the file as a resource)</li>
 *   <li>use jks for the truststoreType-attribute</li>
 *   <li>restart your application</li>
 *   <li>instead of IBM ikeyman you can use the standard java tool <code>keytool</code> as follows: 
 *      <code>keytool -import -alias <i>yourAlias</i> -file <i>pathToSavedCertificate</i></code></li>
 * </ul>
 * <p>
 * Note 4:
 * In case <code>cannot create or initialize SocketFactory: (IOException) Unable to verify MAC</code>-exceptions are thrown,
 * please check password or authAlias configuration of the correspondig certificate. 
 *  
 * </p>
 * @author Gerrit van Brakel
 * @since 4.2c
 */
public class HttpSender extends SenderWithParametersBase implements HasPhysicalDestination {

	private String url;
	private String urlParam="url";
	private String methodType="GET"; // GET or POST
	private String contentType="text/html; charset="+Misc.DEFAULT_INPUT_STREAM_ENCODING;

	private int timeout=10000;
	private int maxConnections=10;
	
	private int maxExecuteRetries=1;

	private String authAlias;
	private String userName;
	private String password;

	private String proxyHost;
	private int    proxyPort=80;
	private String proxyAuthAlias;
	private String proxyUserName;
	private String proxyPassword;
	private String proxyRealm=null;

	private String certificate;
	private String certificateAuthAlias;
	private String certificatePassword;
	private String keystoreType="pkcs12";
	private String keyManagerAlgorithm=null;
	private String truststore=null;
	private String truststoreAuthAlias;
	private String truststorePassword=null;
	private String truststoreType="jks";
	private String trustManagerAlgorithm=null;
	private String inputMessageParam=null;
	
	private boolean allowSelfSignedCertificates = false;
	private boolean verifyHostname=true;
	private boolean jdk13Compatibility=false;
	private boolean followRedirects=true;
	private boolean staleChecking=true;
	private boolean encodeMessages=false;
	private boolean paramsInUrl=true;
	private boolean ignoreRedirects=false;

	protected Parameter urlParameter;
	
	protected URI staticUri;
	private MultiThreadedHttpConnectionManager connectionManager;
	protected HttpClient httpclient;
	protected HostConfiguration hostconfigurationBase; // hostconfiguration shared by all requests
	protected HttpState httpState;					   // global http state	
	private Credentials credentials;
	
	private AuthSSLProtocolSocketFactoryBase socketfactory =null;
	
	private Set parametersToSkip=new HashSet();


	protected void addParameterToSkip(Parameter param) {
		if (param!=null) {
			parametersToSkip.add(param);
		}
	}
	
	protected URI getURI(String url) throws URIException {
		URI uri = new URI(url);

		if (uri.getPath()==null) {
			uri.setPath("/");
		}

		log.info(getLogPrefix()+"created uri: scheme=["+uri.getScheme()+"] host=["+uri.getHost()+"] path=["+uri.getPath()+"]");
		return uri;
	}
	
	protected int getPort(URI uri) {
		int port = uri.getPort();
		if (port<1) {
			try {
				log.debug(getLogPrefix()+"looking up protocol for scheme ["+uri.getScheme()+"]");
				port = Protocol.getProtocol(uri.getScheme()).getDefaultPort();
			} catch (IllegalStateException e) {
				log.debug(getLogPrefix()+"protocol for scheme ["+uri.getScheme()+"] not found, setting port to 80",e);
				port=80; 
			}
		}
		return port;
	}
	
	public void configure() throws ConfigurationException {
		super.configure();
		
		if (!getMethodType().equals("POST")) {
			if (!isParamsInUrl()) {
				throw new ConfigurationException(getLogPrefix()+"paramsInUrl can only be set to false for methodType POST");
			}
			if (StringUtils.isNotEmpty(getInputMessageParam())) {
				throw new ConfigurationException(getLogPrefix()+"inputMessageParam can only be set for methodType POST");
			}
		}
		
//		System.setProperty("javax.net.debug","all"); // normaal Java
//		System.setProperty("javax.net.debug","true"); // IBM java
		httpclient = new HttpClient();
		httpclient.setTimeout(getTimeout());
		httpclient.setConnectionTimeout(getTimeout());
		httpclient.setHttpConnectionFactoryTimeout(getTimeout());
		hostconfigurationBase = httpclient.getHostConfiguration();		           
		
		if (paramList!=null) {
			paramList.configure();
			if (StringUtils.isNotEmpty(getUrlParam())) {
				urlParameter = paramList.findParameter(getUrlParam());
				addParameterToSkip(urlParameter);
			}
		}
		if (getMaxConnections()<=0) {
			throw new ConfigurationException(getLogPrefix()+"maxConnections is set to ["+getMaxConnections()+"], which is not enough for adequate operation");
		}
		try {
			if (urlParameter==null) {
				if (StringUtils.isEmpty(getUrl())) {
					throw new ConfigurationException(getLogPrefix()+"url must be specified, either as attribute, or as parameter");
				}
				staticUri=getURI(getUrl());
			}

			URL certificateUrl=null;
			URL truststoreUrl=null;
	
			if (!StringUtils.isEmpty(getCertificate())) {
				certificateUrl = ClassUtils.getResourceURL(this, getCertificate());
				if (certificateUrl==null) {
					throw new ConfigurationException(getLogPrefix()+"cannot find URL for certificate resource ["+getCertificate()+"]");
				}
				log.info(getLogPrefix()+"resolved certificate-URL to ["+certificateUrl.toString()+"]");
			}
			if (!StringUtils.isEmpty(getTruststore())) {
				truststoreUrl = ClassUtils.getResourceURL(this, getTruststore());
				if (truststoreUrl==null) {
					throw new ConfigurationException(getLogPrefix()+"cannot find URL for truststore resource ["+getTruststore()+"]");
				}
				log.info(getLogPrefix()+"resolved truststore-URL to ["+truststoreUrl.toString()+"]");
			}

			
			if (certificateUrl!=null || truststoreUrl!=null || allowSelfSignedCertificates) {
				//AuthSSLProtocolSocketFactoryBase socketfactory ;
				try {
					CredentialFactory certificateCf = new CredentialFactory(getCertificateAuthAlias(), null, getCertificatePassword());
					CredentialFactory truststoreCf  = new CredentialFactory(getTruststoreAuthAlias(),  null, getTruststorePassword());
					if (isJdk13Compatibility()) {
						socketfactory = new AuthSSLProtocolSocketFactoryForJsse10x(
							certificateUrl, certificateCf.getPassword(), getKeystoreType(), getKeyManagerAlgorithm(),
							truststoreUrl,  truststoreCf.getPassword(),  getTruststoreType(), getTrustManagerAlgorithm(),
							isAllowSelfSignedCertificates(), isVerifyHostname());
					} else {
						socketfactory = new AuthSSLProtocolSocketFactory(
							certificateUrl, certificateCf.getPassword(), getKeystoreType(), getKeyManagerAlgorithm(),
							truststoreUrl,  truststoreCf.getPassword(),  getTruststoreType(), getTrustManagerAlgorithm(),
							isAllowSelfSignedCertificates(), isVerifyHostname());
					}
					socketfactory.initSSLContext();	
				} catch (Throwable t) {
					throw new ConfigurationException(getLogPrefix()+"cannot create or initialize SocketFactory",t);
				}
			}
			httpState = new HttpState();
			
			CredentialFactory cf = new CredentialFactory(getAuthAlias(), getUserName(), getPassword());
			if (!StringUtils.isEmpty(cf.getUsername())) {
				httpState.setAuthenticationPreemptive(true);
				credentials = new UsernamePasswordCredentials(cf.getUsername(), cf.getPassword());
			}
			if (StringUtils.isNotEmpty(getProxyHost())) {
				CredentialFactory pcf = new CredentialFactory(getProxyAuthAlias(), getProxyUserName(), getProxyPassword());
				hostconfigurationBase.setProxy(getProxyHost(), getProxyPort());
				if (StringUtils.isNotEmpty(pcf.getUsername())) {
					httpState.setProxyCredentials(getProxyRealm(), getProxyHost(),
					new UsernamePasswordCredentials(pcf.getUsername(), pcf.getPassword()));
				}
			}
	

		} catch (URIException e) {
			throw new ConfigurationException(getLogPrefix()+"cannot interprete uri ["+getUrl()+"]");
		}

	}

	public void open() {
//		connectionManager = new IbisMultiThreadedHttpConnectionManager();
		connectionManager = new HttpConnectionManager(0,getName());
		connectionManager.setMaxConnectionsPerHost(getMaxConnections());
		log.debug(getLogPrefix()+"set up connectionManager, stale checking ["+connectionManager.isConnectionStaleCheckingEnabled()+"]");
		if (connectionManager.isConnectionStaleCheckingEnabled() != isStaleChecking()) {
			log.info(getLogPrefix()+"set up connectionManager, setting stale checking ["+isStaleChecking()+"]");
			connectionManager.setConnectionStaleCheckingEnabled(isStaleChecking());
		}
		httpclient.setHttpConnectionManager(connectionManager);
	}

	public void close() {
		connectionManager.shutdown();
		connectionManager=null;
	}

	public boolean isSynchronous() {
		return true;
	}

	protected boolean appendParameters(boolean parametersAppended, StringBuffer path, ParameterValueList parameters) {
		if (parameters!=null) {
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"appending ["+parameters.size()+"] parameters");
		}
		for(int i=0; i<parameters.size(); i++) {
			if (parametersToSkip.contains(paramList.get(i))) {
				if (log.isDebugEnabled()) log.debug(getLogPrefix()+"skipping ["+paramList.get(i)+"]");
				continue;
			}
			if (parametersAppended) {
				path.append("&");
			} else {
				path.append("?");
				parametersAppended = true;
			}
			ParameterValue pv = parameters.getParameterValue(i);
			String parameterToAppend=pv.getDefinition().getName()+"="+URLEncoder.encode(pv.asStringValue(""));
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"appending parameter ["+parameterToAppend+"]");
			path.append(parameterToAppend);
		}
		return parametersAppended;
	}

	protected HttpMethod getMethod(URI uri, String message, ParameterValueList parameters) throws SenderException {
		try { 
			boolean queryParametersAppended = false;
			if (isEncodeMessages()) {
				message = URLEncoder.encode(message);
			}
			
			StringBuffer path = new StringBuffer(uri.getPath());
			if (!StringUtils.isEmpty(uri.getQuery())) {
				path.append("?"+uri.getQuery());
				queryParametersAppended = true;
			}
			
			if (getMethodType().equals("GET")) {
				if (parameters!=null) {
					queryParametersAppended = appendParameters(queryParametersAppended,path,parameters);
					if (log.isDebugEnabled()) log.debug(getLogPrefix()+"path after appending of parameters ["+path.toString()+"]");
				}
				GetMethod result = new GetMethod(path+(parameters==null? message:""));
				if (log.isDebugEnabled()) log.debug(getLogPrefix()+"HttpSender constructed GET-method ["+result.getQueryString()+"]");
				return result;
			} 
			if (getMethodType().equals("POST")) {
				PostMethod postMethod = new PostMethod(path.toString());
				if (StringUtils.isNotEmpty(getContentType())) {
					postMethod.setRequestHeader("Content-Type",getContentType());
				}
				if (parameters!=null) {
					StringBuffer msg = new StringBuffer(message);
					appendParameters(true,msg,parameters);
					if (StringUtils.isEmpty(message) && msg.length()>1) {
						message=msg.substring(1);
					} else {
						message=msg.toString();
					}
				}
				postMethod.setRequestBody(message);
			
				return postMethod;
			}
			throw new SenderException("unknown methodtype ["+getMethodType()+"], must be either POST or GET");
		} catch (URIException e) {
			throw new SenderException(getLogPrefix()+"cannot find path from url ["+getUrl()+"]", e);
		}

	}

	protected PostMethod getPostMethodWithParamsInBody(URI uri, String message, ParameterValueList parameters) throws SenderException {
		try {
			PostMethod hmethod = new PostMethod(uri.getPath());
			if (StringUtils.isNotEmpty(getInputMessageParam())) {
				hmethod.addParameter(getInputMessageParam(),message);
				if (log.isDebugEnabled()) log.debug(getLogPrefix()+"appended parameter ["+getInputMessageParam()+"] with value ["+message+"]");
			}
			if (parameters!=null) {
				for(int i=0; i<parameters.size(); i++) {
					ParameterValue pv = parameters.getParameterValue(i);
					String name = pv.getDefinition().getName();
					String value = pv.asStringValue("");
					hmethod.addParameter(name,value);
					if (log.isDebugEnabled()) log.debug(getLogPrefix()+"appended parameter ["+name+"] with value ["+value+"]");
				}
			}
			return hmethod;
		} catch (URIException e) {
			throw new SenderException(getLogPrefix()+"cannot find path from url ["+getUrl()+"]", e);
		}
	}
	
	public String extractResult(HttpMethod httpmethod) throws SenderException, IOException {
		int statusCode = httpmethod.getStatusCode();
		boolean ok = false;
		if (statusCode==HttpServletResponse.SC_OK) {
			ok = true;
		} else { 
			if (isIgnoreRedirects()) {
				if (statusCode==HttpServletResponse.SC_MOVED_PERMANENTLY || statusCode==HttpServletResponse.SC_MOVED_TEMPORARILY || statusCode==HttpServletResponse.SC_TEMPORARY_REDIRECT) {
					ok = true;
				}
			}
		}
		if (!ok) {
			throw new SenderException(getLogPrefix()+"httpstatus "+statusCode+": "+httpmethod.getStatusText());
		}
		//return httpmethod.getResponseBodyAsString();
		return getResponseBodyAsString(httpmethod);
	}

	public String getResponseBodyAsString(HttpMethod httpmethod) throws IOException {
		String charset = ((HttpMethodBase)httpmethod).getResponseCharSet();
		if (log.isDebugEnabled()) log.debug(getLogPrefix()+"response body uses charset ["+charset+"]");
		InputStream is = httpmethod.getResponseBodyAsStream();
		String responseBody = Misc.streamToString(is,"\n",charset,false);
		int rbLength = responseBody.length();
		long rbSizeError = Misc.getResponseBodySizeErrorByDefault();
		if (rbLength >= rbSizeError) {
			log.error(getLogPrefix()+"retrieved result size [" +Misc.toFileSize(rbLength)+"] exceeds ["+Misc.toFileSize(rbSizeError)+"]");
		} else {
			long rbSizeWarn = Misc.getResponseBodySizeWarnByDefault();
			if (rbLength >= rbSizeWarn) {
				log.warn(getLogPrefix()+"retrieved result size [" +Misc.toFileSize(rbLength)+"] exceeds ["+Misc.toFileSize(rbSizeWarn)+"]");
			}
		}
		return responseBody;
	}

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		ParameterValueList pvl = null;
		try {
			if (prc !=null && paramList !=null) {
				pvl=prc.getValues(paramList);
			}
		} catch (ParameterException e) {
			throw new SenderException(getLogPrefix()+"Sender ["+getName()+"] caught exception evaluating parameters",e);
		}
		URI uri;
		HttpMethod httpmethod;
		HostConfiguration hostconfiguration=new HostConfiguration(hostconfigurationBase);
		try {
			if (urlParameter!=null) {
				String url=(String)pvl.getParameterValue(getUrlParam()).getValue();
				uri=getURI(url);
			} else {
				uri=staticUri;
			}

			if (!isParamsInUrl()) {
				httpmethod=getPostMethodWithParamsInBody(uri, message, pvl);
			} else {
				httpmethod=getMethod(uri, message, pvl);
				if (!"POST".equals(getMethodType())) {
					httpmethod.setFollowRedirects(isFollowRedirects());
				}
			}

			int port = getPort(uri);
		
			if (socketfactory!=null && "https".equals(uri.getScheme())) {
				Protocol authhttps = new Protocol(uri.getScheme(), socketfactory, port);
				hostconfiguration.setHost(uri.getHost(),port,authhttps);
			} else {
				hostconfiguration.setHost(uri.getHost(),port,uri.getScheme());
			}
			log.info(getLogPrefix()+"configured httpclient for host ["+hostconfiguration.getHostURL()+"]");
		
			if (credentials!=null) {
				httpState.setCredentials(null, uri.getHost(), credentials);
			}
		} catch (URIException e) {
			throw new SenderException(e);
		}
		
		String result = null;
		int statusCode = -1;
		int count=getMaxExecuteRetries();
		String msg = null;
		while (count-->=0 && statusCode==-1) {
			try {
				if (log.isDebugEnabled()) log.debug(getLogPrefix()+"executing method");
				statusCode = httpclient.executeMethod(hostconfiguration,httpmethod,httpState);
				if (log.isDebugEnabled()) log.debug(getLogPrefix()+"executed method");
				
				if (statusCode!=HttpServletResponse.SC_OK) {
					StatusLine statusline = httpmethod.getStatusLine();
					if (statusline!=null) { 
						log.warn(getLogPrefix()+"status ["+statusline.toString()+"]");
					} else {
						log.warn(getLogPrefix()+"no statusline found");
					}
				} else {
					if (log.isDebugEnabled()) log.debug(getLogPrefix()+"status ["+statusCode+"]");
				}
				result = extractResult(httpmethod);	
				if (log.isDebugEnabled()) log.debug(getLogPrefix()+"retrieved result ["+result+"]");
			} catch (HttpException e) {
				Throwable throwable = e.getCause();
				String cause = null;
				if (throwable!=null) {
					cause = throwable.toString();
				}
				msg = e.getMessage();
				log.warn(getLogPrefix()+"httpException with message [" + msg + "] and cause [" + cause + "], executeRetries left [" + count + "]");
			} catch (IOException e) {
				httpmethod.abort();
				if (e instanceof SocketTimeoutException) {
					throw new TimeOutException(e);
				} 
				throw new SenderException(e);
			} finally {
				httpmethod.releaseConnection();
			}
		}

		if (statusCode==-1){
			if (StringUtils.contains(msg.toUpperCase(), "TIMEOUTEXCEPTION")) {
				//java.net.SocketTimeoutException: Read timed out
				throw new TimeOutException("Failed to recover from timeout exception");
			}
			throw new SenderException("Failed to recover from exception");
		}

		return result;	
	}

	public String sendMessage(String correlationID, String message) throws SenderException, TimeOutException {
		return sendMessage(correlationID, message, null);
	}



	public String getPhysicalDestinationName() {
		if (urlParameter!=null) {
			return "dynamic url";
		}
		return getUrl();
	}



	public String getUrl() {
		return url;
	}
	public void setUrl(String string) {
		url = string;
	}

	public String getUrlParam() {
		return urlParam;
	}
	public void setUrlParam(String urlParam) {
		this.urlParam = urlParam;
	}

	public String getMethodType() {
		return methodType;
	}
	public void setMethodType(String string) {
		methodType = string;
	}

	public void setContentType(String string) {
		contentType = string;
	}
	public String getContentType() {
		return contentType;
	}

	public int getTimeout() {
		return timeout;
	}
	public void setTimeout(int i) {
		timeout = i;
	}

	public int getMaxConnections() {
		return maxConnections;
	}
	public void setMaxConnections(int i) {
		maxConnections = i;
	}

	public int getMaxExecuteRetries() {
		return maxExecuteRetries;
	}
	public void setMaxExecuteRetries(int i) {
		maxExecuteRetries = i;
	}


	public String getAuthAlias() {
		return authAlias;
	}
	public void setAuthAlias(String string) {
		authAlias = string;
	}

	public String getUserName() {
		return userName;
	}
	public void setUserName(String string) {
		userName = string;
	}

	public String getPassword() {
		return password;
	}
	public void setPassword(String string) {
		password = string;
	}

	
	public String getProxyHost() {
		return proxyHost;
	}
	public void setProxyHost(String string) {
		proxyHost = string;
	}

	public int getProxyPort() {
		return proxyPort;
	}
	public void setProxyPort(int i) {
		proxyPort = i;
	}

	public String getProxyAuthAlias() {
		return proxyAuthAlias;
	}
	public void setProxyAuthAlias(String string) {
		proxyAuthAlias = string;
	}

	public String getProxyUserName() {
		return proxyUserName;
	}
	public void setProxyUserName(String string) {
		proxyUserName = string;
	}

	public String getProxyPassword() {
		return proxyPassword;
	}
	public void setProxyPassword(String string) {
		proxyPassword = string;
	}

	public String getProxyRealm() {
		return proxyRealm;
	}
	public void setProxyRealm(String string) {
		proxyRealm = string;
	}


	
	public String getCertificate() {
		return certificate;
	}
	public void setCertificate(String string) {
		certificate = string;
	}

	public String getCertificateAuthAlias() {
		return certificateAuthAlias;
	}
	public void setTruststoreAuthAlias(String string) {
		truststoreAuthAlias = string;
	}

	public String getCertificatePassword() {
		return certificatePassword;
	}
	public void setCertificatePassword(String string) {
		certificatePassword = string;
	}

	public String getKeystoreType() {
		return keystoreType;
	}
	public void setKeystoreType(String string) {
		keystoreType = string;
	}

	public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		this.keyManagerAlgorithm = keyManagerAlgorithm;
	}
	public String getKeyManagerAlgorithm() {
		return keyManagerAlgorithm;
	}

	
	public String getTruststore() {
		return truststore;
	}
	public void setTruststore(String string) {
		truststore = string;
	}

	public String getTruststoreAuthAlias() {
		return truststoreAuthAlias;
	}
	public void setCertificateAuthAlias(String string) {
		certificateAuthAlias = string;
	}

	public String getTruststorePassword() {
		return truststorePassword;
	}
	public void setTruststorePassword(String string) {
		truststorePassword = string;
	}

	public String getTruststoreType() {
		return truststoreType;
	}
	public void setTruststoreType(String string) {
		truststoreType = string;
	}

	public void setTrustManagerAlgorithm(String trustManagerAlgorithm) {
		this.trustManagerAlgorithm = trustManagerAlgorithm;
	}
	public String getTrustManagerAlgorithm() {
		return trustManagerAlgorithm;
	}


	public boolean isVerifyHostname() {
		return verifyHostname;
	}
	public void setVerifyHostname(boolean b) {
		verifyHostname = b;
	}

	public boolean isJdk13Compatibility() {
		return jdk13Compatibility;
	}
	public void setJdk13Compatibility(boolean b) {
		jdk13Compatibility = b;
	}

	public boolean isEncodeMessages() {
		return encodeMessages;
	}
	public void setEncodeMessages(boolean b) {
		encodeMessages = b;
	}

	public boolean isStaleChecking() {
		return staleChecking;
	}
	public void setStaleChecking(boolean b) {
		staleChecking = b;
	}

	public boolean isFollowRedirects() {
		return followRedirects;
	}
	public void setFollowRedirects(boolean b) {
		followRedirects = b;
	}

	public void setAllowSelfSignedCertificates(boolean allowSelfSignedCertificates) {
		this.allowSelfSignedCertificates = allowSelfSignedCertificates;
	}

	public boolean isAllowSelfSignedCertificates() {
		return allowSelfSignedCertificates;
	}

	public void setInputMessageParam(String inputMessageParam) {
		this.inputMessageParam = inputMessageParam;
	}
	public String getInputMessageParam() {
		return inputMessageParam;
	}

	public void setIgnoreRedirects(boolean b) {
		ignoreRedirects = b;
	}
	public boolean isIgnoreRedirects() {
		return ignoreRedirects;
	}

	public void setParamsInUrl(boolean b) {
		paramsInUrl = b;
	}
	public boolean isParamsInUrl() {
		return paramsInUrl;
	}
}
