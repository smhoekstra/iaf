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
package nl.nn.adapterframework.batch;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;


/**
 * Resulthandler that writes the transformed record to a String, that is passed literally to the next Pipe.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.batch.Result2Filewriter</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>Name of the resulthandler</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPrefix(String) prefix}</td><td><i>Deprecated</i> Prefix that has to be written before record, if the record is in another block than the previous record</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSuffix(String) suffix}</td><td><i>Deprecated</i> Suffix that has to be written after the record, if the record is in another block than the next record. <br/>N.B. If a suffix is set without a prefix, it is only used at the end of processing (i.e. at the end of the file) as a final close</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDefault(boolean) default}</td><td>If true, this resulthandler is the default for all RecordHandlingFlow that do not have a handler specified</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOnOpenDocument(String) onOpenDocument}</td><td>String that is written before any data of results is written</td><td>&lt;document name=&quot;#name#&quot;&gt;</td></tr>
 * <tr><td>{@link #setOnCloseDocument(String) onCloseDocument}</td><td>String that is written after all data of results is written</td><td>&lt;/document&gt;</td></tr>
 * <tr><td>{@link #setOnOpenBlock(String) onOpenBlock}</td><td>String that is written before the start of each logical block, as defined in the flow</td><td>&lt;#name#&gt;</td></tr>
 * <tr><td>{@link #setOnCloseBlock(String) onCloseBlock}</td><td>String that is written after the end of each logical block, as defined in the flow</td><td>&lt;/#name#&gt;</td></tr>
 * <tr><td>{@link #setBlockNamePattern(String) blockNamePattern}</td><td>String that is replaced by name of block or name of stream in above strings</td><td>#name#</td></tr>
 * <tr><td>{@link #setBlockByRecordType(boolean) blockByRecordType}</td><td>when set <code>true</code>(default), every group of records, as indicated by {@link IRecordHandler.isNewRecordType RecordHandler.newRecordType} is handled as a block.</td><td>true</td></tr>
 * </table>
 * </p>
 * 
 * @author: Gerrit van Brakel
 * @since   4.7
 * @version $Id$
 */
public class Result2StringWriter extends ResultWriter {
	
	private Map openWriters = Collections.synchronizedMap(new HashMap());
	
	protected Writer createWriter(IPipeLineSession session, String streamId, ParameterResolutionContext prc) throws Exception {
		Writer writer=new StringWriter();
		openWriters.put(streamId,writer);
		return writer;
	}
	
	public Object finalizeResult(IPipeLineSession session, String streamId, boolean error, ParameterResolutionContext prc) throws Exception {
		super.finalizeResult(session,streamId, error, prc);
		StringWriter writer = (StringWriter)getWriter(session,streamId,false, prc);
		String result=null;
		if (writer!=null) {
			result = (writer).getBuffer().toString();
		} 
		return result;		
	}

}
