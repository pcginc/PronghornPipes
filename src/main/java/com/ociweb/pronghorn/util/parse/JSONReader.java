package com.ociweb.pronghorn.util.parse;

import com.ociweb.pronghorn.pipe.ChannelReader;

public interface JSONReader {

	long getLong(byte[] field, ChannelReader reader);
	<A extends Appendable> A getText(byte[] field, ChannelReader reader, A target);
	boolean getBoolean(byte[] field, ChannelReader reader);
	boolean wasNull(ChannelReader reader);
	
	
	
	
}
