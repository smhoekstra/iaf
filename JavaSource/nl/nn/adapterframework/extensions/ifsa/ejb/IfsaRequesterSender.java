/*
 * $Log: IfsaRequesterSender.java,v $
 * Revision 1.1.2.1  2007-10-25 15:03:44  europe\M00035F
 * Begin work on implementing IFSA-EJB
 *
 * 
 */

package nl.nn.adapterframework.extensions.ifsa.ejb;

import com.ing.ifsa.exceptions.IFSAException;
import com.ing.ifsa.api.BusinessMessage;
import com.ing.ifsa.api.Connection;
import com.ing.ifsa.api.ConnectionManager;
import com.ing.ifsa.api.FireForgetAccessBean;
import com.ing.ifsa.api.RequestReplyAccessBean;
import com.ing.ifsa.api.ServiceReply;
import com.ing.ifsa.api.ServiceRequest;
import com.ing.ifsa.api.ServiceURI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.extensions.ifsa.IfsaMessageProtocolEnum;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.LogUtil;
import org.apache.log4j.Logger;

/**
 * IFSA Request sender for FF and RR requests implemented using the IFSA
 * J2EE api.
 * 
 * @author Tim van der Leeuw
 * @version Id
 */
public class IfsaRequesterSender extends IfsaEjbBase implements ISenderWithParameters, INamedObject, HasPhysicalDestination {
    // TODO: Pull up most properties to new abstract base class
    protected Logger log = LogUtil.getLogger(this);

    protected ParameterList paramList = null;
    private String name;
    private String applicationId;
    private String serviceId;
    // TODO: Do we need polishedServiceId? Not using now; discus w/Gerrit; also risk of replacing the ':' in URL-Protocol? (IFSA://Service/...)
    private String polishedServiceId=null;
    private IfsaMessageProtocolEnum messageProtocol;

    private long timeOut = -1; // when set (>=0), overrides IFSA-expiry

    public void configure() throws ConfigurationException {
        if (paramList!=null) {
            paramList.configure();
        }
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void open() throws SenderException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void close() throws SenderException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    protected Map convertParametersToMap(ParameterResolutionContext prc) throws SenderException {
        ParameterValueList paramValueList;
        try {
            paramValueList = prc.getValues(paramList);
        } catch (ParameterException e) {
            throw new SenderException(getLogPrefix() + "caught ParameterException in sendMessage() determining serviceId", e);
        }
        Map params = new HashMap();
        if (paramValueList != null && paramList != null) {
            for (int i = 0; i < paramList.size(); i++) {
                String key = paramList.getParameter(i).getName();
                String value = paramValueList.getParameterValue(i).asStringValue(null);
                params.put(key, value);
            }
        }
        return params;
    }

    protected String getLogPrefix() {
        return "IfsaRequester["+ getName()+ 
                "] of Application [" + getApplicationId()+"] ";  
    }

    public boolean isSynchronous() {
        return getMessageProtocolEnum().equals(IfsaMessageProtocolEnum.REQUEST_REPLY);
    }

    public String sendMessage(String dummyCorrelationId, String message) throws SenderException, TimeOutException {
        return sendMessage(dummyCorrelationId, message, (Map)null);
    }

    public String sendMessage(String dummyCorrelationId, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
        Map params = convertParametersToMap(prc);
        return sendMessage(dummyCorrelationId, message, params);
    }

    /**
     * Execute a request to the IFSA service.
     * @return in Request/Reply, the retrieved message or TIMEOUT, otherwise null
     */
    public String sendMessage(String dummyCorrelationId, String message, Map params) throws SenderException, TimeOutException {
        Connection conn = null;
        Map udzMap = null;
        
        try {
            String realServiceId;
            // Extract parameters
            if (params != null && params.size() > 0) {
                // Use first param as serviceId
                realServiceId = (String)params.get("serviceId");
                if (realServiceId == null) {
                        realServiceId = getServiceId();
                }
                String occurrence = (String)params.get("occurrence");
                if (occurrence != null) {
                    int i = realServiceId.indexOf('/', realServiceId.indexOf('/', realServiceId.indexOf('/', realServiceId.indexOf('/') + 1) + 1) + 1);
                    int j = realServiceId.indexOf('/', i + 1);
                    realServiceId = realServiceId.substring(0, i + 1) + occurrence + realServiceId.substring(j);
                }

                // Use remaining params as outgoing UDZs
                udzMap = new HashMap();
                udzMap.putAll(params);
                udzMap.remove("serviceId");
                udzMap.remove("occurrence");
            } else {
                realServiceId = getServiceId();
            }
            
            // Open connection to the Application ID
            conn = ConnectionManager.getConnection(getApplicationId());

            // Create the request, and set the Service URI to the Service ID
            ServiceRequest request = new ServiceRequest(new BusinessMessage(message));
            request.setServiceURI(new ServiceURI(realServiceId));
            addUdzMapToRequest(udzMap, request);
            if (isSynchronous()) {
                // RR handling
                if (timeOut > 0) {
                    request.setTimeout(timeOut);
                }
                RequestReplyAccessBean rrBean = RequestReplyAccessBean.getInstance();
                ServiceReply reply = rrBean.sendReceive(conn, request);
                return reply.getBusinessMessage().getText();
            } else {
                // FF handling
                FireForgetAccessBean ffBean = FireForgetAccessBean.getInstance();
                ffBean.send(conn, request);
                return null;
            }
        } catch (com.ing.ifsa.exceptions.TimeoutException toe) {
            throw new TimeOutException(toe);
        } catch (IFSAException e) {
            throw new SenderException(e);
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }
        
    protected void addUdzMapToRequest(Map udzMap, ServiceRequest request) {
        if (udzMap == null) {
            return;
        }
        for (Iterator it = udzMap.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry)it.next();
            request.setUserDefinedZone(entry.getKey(), entry.getValue());
        }
    }

    public void addParameter(Parameter p) {
        if (paramList==null) {
            paramList=new ParameterList();
        }
        paramList.add(p);
    }

    public String getPhysicalDestinationName() {
        String result = null;

        try {
            result = getServiceId();
//            log.debug("obtaining connection and servicequeue for "+result);
//            if (getServiceQueue() != null) {
//                result += " ["+ getServiceQueue().getQueueName()+"]";
//            }
        } catch (Throwable t) {
            log.warn(getLogPrefix()+"got exception in getPhysicalDestinationName", t);
        }
        return result;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getMessageProtocol() {
        return messageProtocol.getName();
    }
    
    public IfsaMessageProtocolEnum getMessageProtocolEnum() {
        return messageProtocol;
    }

    /**
     * Method logs a warning when the newMessageProtocol is not <code>FF</code>
     * or <code>RR</code>.
     * <p>When the messageProtocol equals to FF, transacted is set to true</p>
     * <p>Creation date: (08-05-2003 9:03:53)</p>
     * @see IfsaMessageProtocolEnum
     * @param newMessageProtocol String
     */
    public void setMessageProtocol(String newMessageProtocol) {
        if (null==IfsaMessageProtocolEnum.getEnum(newMessageProtocol)) {
            throw new IllegalArgumentException(getLogPrefix()+
            "illegal messageProtocol ["
                + newMessageProtocol
                + "] specified, it should be one of the values "
                + IfsaMessageProtocolEnum.getNames());

        }
        messageProtocol = IfsaMessageProtocolEnum.getEnum(newMessageProtocol);
        log.debug(getLogPrefix()+"message protocol set to "+messageProtocol.getName());
    }
 
    public String getPolishedServiceId() {
        return polishedServiceId;
    }

    public void setPolishedServiceId(String polishedServiceId) {
        this.polishedServiceId = polishedServiceId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public long getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(long timeOut) {
        this.timeOut = timeOut;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
