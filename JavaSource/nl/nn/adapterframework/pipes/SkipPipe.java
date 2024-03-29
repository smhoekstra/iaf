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
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

/**
 * Skip a number of bytes or characters from the input. 
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setSkip(int) skip}</td><td>number of bytes (for byte array input) or characters (for String input) to skip. An empty byte array or String is returned when skip is larger then the length of the input</td><td>0</td></tr>
 * <tr><td>{@link #setLength(int) length}</td><td>if length>=0 only these number of bytes (for byte array input) or characters (for String input) is returned.</td><td>-1</td></tr>
 * </table>
 * </p>
 * 
 * @author Jaco de Groot (***@dynasol.nl)
 * @version $Id$
 *
 */
public class SkipPipe extends FixedForwardPipe {
	public static final String version="$RCSfile: SkipPipe.java,v $ $Revision: 1.5 $ $Date: 2012-06-01 10:52:49 $";

	private int skip = 0;
	private int length = -1;
	
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		try {
			Object result = "SkipPipe doesn't work for this type of object";
			if (input instanceof String) {
				String stringInput = (String)input;
				if (skip > stringInput.length()) {
					result = "";
				} else {
					if (length >= 0 && length < stringInput.length() - skip) {
						result = stringInput.substring(skip, skip + length);
					} else {
						result = stringInput.substring(skip);
					}
				}
			} else if (input instanceof byte[]) {
				byte[] bytesInput = (byte[])input;
				byte[] bytesResult;
				if (skip > bytesInput.length) {
					bytesResult = new byte[0];
				} else {
					if (length >= 0 && length < bytesInput.length - skip) {
						bytesResult = new byte[length];
						for (int i = 0; i < length; i++) {
							bytesResult[i] = bytesInput[skip + i];
						}
					} else {
						bytesResult = new byte[bytesInput.length - skip];
						for (int i = 0; i < bytesResult.length; i++) {
							bytesResult[i] = bytesInput[skip + i];
						}
					}
				}
				result = bytesResult;
			}
			return new PipeRunResult(getForward(), result);
		} catch(Exception e) {
			throw new PipeRunException(this, "Error while transforming input", e); 
		}
	}

	/**
	 * @param skip  the number of bytes to skip
	 */
	public void setSkip(int skip) {
		this.skip = skip;
	}

	public void setLength(int length) {
		this.length = length;
	}
}
