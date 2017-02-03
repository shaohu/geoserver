package org.geoserver.wfs.format.ext;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.json.GeoJSONGetFeatureResponse;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.tukaani.xz.FilterOptions;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;

public class Json7ZOutputFormat extends GeoJSONGetFeatureResponse {

	public Json7ZOutputFormat(GeoServer geoServer, String format) {
		super(geoServer, format);
	}

	protected void write(FeatureCollectionResponse featureCollection, OutputStream output,
			Operation getFeature) throws IOException {
		Date date_getRequest = new Date();
		ByteArrayOutputStream jsonOutputStream = new ByteArrayOutputStream();
		super.write(featureCollection, jsonOutputStream, getFeature);
		//System.out.println(new String(jsonOutputStream.toByteArray()));
		
		LZMA2Options options = new LZMA2Options();
		XZOutputStream xzout = new XZOutputStream(output, options);
		InputStream is = new ByteArrayInputStream(jsonOutputStream.toByteArray());

		BufferedInputStream stream = new BufferedInputStream(is);
		byte[] buf = new byte['?'];
		int streamlength = 0;
		FilterOptions[] options1 = { options };
		int size;
		while ((size = stream.read(buf)) != -1) {
			xzout.write(buf, 0, size);
			streamlength += size;
		}
		xzout.flush();
		xzout.close();
		Date date_end = new Date();
	}
	
	@Override
	public String getMimeType(Object value, Operation operation) {
		return "application.json.7z/7z; subtype=json";
	}
}