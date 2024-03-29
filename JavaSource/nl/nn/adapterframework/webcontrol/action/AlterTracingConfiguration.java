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
package nl.nn.adapterframework.webcontrol.action;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.pipes.AbstractPipe;
import nl.nn.adapterframework.receivers.ReceiverBase;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AlterTracingConfiguration extends ActionBase {
	public static final String version="$RCSfile: AlterTracingConfiguration.java,v $ $Revision: 1.3 $ $Date: 2011-11-30 13:51:46 $";

	public ActionForward execute(
		ActionMapping mapping,
		ActionForm form,
		HttpServletRequest request,
		HttpServletResponse response)
		throws ServletException {
		initAction(request);
		if (null == config) {
			return (mapping.findForward("noconfig"));
		}

		DynaActionForm alterGimTemplateForm =
			getPersistentForm(mapping, form, request);

		initAction(request);
		String adapterName = request.getParameter("adapter");
		String pipeName = request.getParameter("pipe");
		String receiverName = request.getParameter("receiver");

		// Report any errors we have discovered back to the original form
		if (!errors.isEmpty()) {
			saveErrors(request, errors);
		}

		int beforeEvent=-2;
		int afterEvent=-2;
		int exceptionEvent=-2;

		Adapter adapter = (Adapter) config.getRegisteredAdapter(adapterName);
		if (pipeName == null) {
			IReceiver receiver = adapter.getReceiverByName(receiverName);

			if (receiver instanceof ReceiverBase) {
				ReceiverBase receiverb = (ReceiverBase)receiver;
				beforeEvent = receiverb.getBeforeEvent();
				afterEvent = receiverb.getAfterEvent();
				exceptionEvent = receiverb.getExceptionEvent();
			}
			pipeName = "";
		} else {
			IPipe pipe = adapter.getPipeLine().getPipe(pipeName);

			if (pipe instanceof AbstractPipe) {
				AbstractPipe ap = (AbstractPipe)pipe;
				beforeEvent = ap.getBeforeEvent();
				afterEvent = ap.getAfterEvent();
				exceptionEvent = ap.getExceptionEvent();
			}
			receiverName = "";
		}

		alterGimTemplateForm.set("adapterName", adapterName);
		alterGimTemplateForm.set("receiverName", receiverName);
		alterGimTemplateForm.set("pipeName", pipeName);
		alterGimTemplateForm.set("beforeEvent", new Integer(beforeEvent));
		alterGimTemplateForm.set("afterEvent", new Integer(afterEvent));
		alterGimTemplateForm.set("exceptionEvent", new Integer(exceptionEvent));

		// Forward control to the specified success URI
		log.debug("forward to success");
		return (mapping.findForward("success"));

	}
}
