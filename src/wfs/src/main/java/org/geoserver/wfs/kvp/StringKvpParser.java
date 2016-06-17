package org.geoserver.wfs.kvp;

import org.geoserver.ows.KvpParser;

/**
 * implemented by Hu Shao hu.shao@asu.edu
 * get the value as it is and return back
 * @author hushao
 *
 */
public class StringKvpParser extends KvpParser {

	public StringKvpParser(String key, Class binding) {
		super(key, binding);
	}

	@Override
	public Object parse(String value) throws Exception {
		return value;
	}

}
