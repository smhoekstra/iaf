/*
 * $Log: DelphiStringRecordReader.java,v $
 * Revision 1.1  2010-05-03 17:03:06  L190409
 * IInputstreamReader-classes to enable reading Delphi String records
 *
 */
package nl.nn.adapterframework.batch;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;

/**
 * 
 * @author  Gerrit van Brakel
 * @since   4.10
 * @version Id
 */
public class DelphiStringRecordReader extends Reader {
	protected Logger log = LogUtil.getLogger(this);

	private InputStream in;
	private String charsetName;
	private int stringLength;
	private int stringsPerRecord; // 0 means read till end of file
	private String separator;
	
	private StringBuffer buffer;
	private int bufferLen=0;
	private int bufferPos=0;
	boolean eof=false;
	
	private final boolean trace=false;

	public DelphiStringRecordReader(InputStream in, String charsetName, int stringLength, int stringsPerRecord, String separator) {
		super();
		this.in=in;
		this.charsetName=charsetName;
		this.stringLength=stringLength;
		this.stringsPerRecord=stringsPerRecord;
		this.separator=separator;
	}
	
	/*
	 * Fill buffer if empty, then copy characters as required.  
	 */
	public int read(char[] cbuf, int off, int len) throws IOException {
		if (buffer==null || bufferPos>=bufferLen) {
			fillBuffer();
		}
		if (buffer==null) {
			return -1;
		}
		int bytesRead=0;
		while (bufferPos<bufferLen && bytesRead++<len) {
			cbuf[off++]=buffer.charAt(bufferPos++);
		}
		return bytesRead;
	}

	public void close() throws IOException {
		in.close();
	}

	/*
	 * read a single string from the input, then skip to stringLength.
	 */
	private String readString() throws IOException {
		int len;
		len=in.read(); // first read the byte that holds the length of the string
		if (len<0) {
			return null;
		}
		if (trace && log.isDebugEnabled()) log.debug("read byte for string length ["+len+"]");
		byte[] buf=new byte[len]; // allocate space for the bytes of the string
		int bytesToRead=len;
		int pos=0;
		while (bytesToRead>0) {
			int bytesRead = in.read(buf,pos,bytesToRead);
			if (bytesRead>0) {
				pos+=bytesRead;
				bytesToRead-=bytesRead;
			} else {
				String currentResult=null;
				try {
					currentResult=new String(buf,charsetName);
				} catch (Exception e) {
					currentResult=e.getClass().getName()+": "+e.getMessage();
				}
				throw new EOFException("unexpected EOF after reading ["+pos+"] bytes of a string of length ["+len+"], current result ["+currentResult+"]");
			}
		}
		if (pos<stringLength) {
			if (trace && log.isDebugEnabled()) log.debug("skipping ["+(stringLength-pos)+"] bytes");		
			in.skip(stringLength-pos);
		}
		String result=new String(buf,charsetName);
		if (trace && log.isDebugEnabled()) log.debug("read string ["+result+"]");
		return result;
	}

	/*
	 * accumulate strings in buffer.
	 */
	private void fillBuffer() throws IOException {
		int stringsRead=0;
		buffer=new StringBuffer();
		while (!eof && (stringsPerRecord==0 || stringsRead<stringsPerRecord)) {
			String part=readString();
			if (part==null) {
				eof=true; 
			} else {
				buffer.append(part).append(separator);
				stringsRead++;
			}
		}
		if (stringsRead==0) {
			buffer=null;
		} else {
			bufferLen=buffer.length();
			bufferPos=0;
		}
	}

}