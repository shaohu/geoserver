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
import org.geoserver.wfs.format.ext.SevenZip.Compression.LZMA.Encoder;
import org.geoserver.wfs.json.GeoJSONGetFeatureResponse;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.tukaani.xz.FilterOptions;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;


public class JsonLZMAOutputFormat extends GeoJSONGetFeatureResponse {

	public JsonLZMAOutputFormat(GeoServer geoServer, String format) {
		super(geoServer, format);
	}

	protected void write(FeatureCollectionResponse featureCollection, OutputStream output,
			Operation getFeature) throws IOException {
		Date date_getRequest = new Date(); 
		ByteArrayOutputStream jsonOutputStream = new ByteArrayOutputStream();
		super.write(featureCollection, jsonOutputStream, getFeature);
		//System.out.println(new String(jsonOutputStream.toByteArray()));
		Date date_begin = new Date();
		byte[] inputBytes = jsonOutputStream.toByteArray();
    	java.io.BufferedInputStream inStream  = new java.io.BufferedInputStream(new ByteArrayInputStream(inputBytes));
		java.io.BufferedOutputStream outStream = new java.io.BufferedOutputStream(output);
    	
		Encoder encoder = new Encoder();
		encoder.WriteCoderProperties(outStream);
		long fileSize = inputBytes.length;
		for (int i = 0; i < 8; i++)
			outStream.write((int)(fileSize >>> (8 * i)) & 0xFF);
    	encoder.Code(inStream, outStream, -1, -1, null);
    	outStream.flush();
    	outStream.close();
    	Date date_end = new Date();
        System.out.println("Time for LZMA compression = "+(date_end.getTime() - date_begin.getTime()));
//        System.out.println("Size for LZMA compression = "+(inputBytes.length));
        TimeUsedForDataPreparingExport.timeUsedForDataPreparing = (int)(date_end.getTime()-date_getRequest.getTime());
	}
	
	@Override
	public String getMimeType(Object value, Operation operation) {
		return "application.json.lzma/lzma; subtype=json";
	}
}
