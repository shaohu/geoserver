package org.geoserver.wfs.format.ext;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.xml.GML3OutputFormat;
import org.geoserver.wfs.xml.v1_1_0.WFSConfiguration;

import org.geoserver.wfs.format.ext.SevenZip.Compression.LZMA.Encoder;

public class GML3LZMAOutputFormat extends GML3OutputFormat {

	public GML3LZMAOutputFormat(GeoServer geoServer, WFSConfiguration configuration) {
		super(new HashSet(Arrays.asList(new Object[]{"gml3-lzma", "application/xhtml+xml;"})), 
				geoServer, configuration);
	}
	
	@Override
	protected void write(FeatureCollectionResponse featureCollection, OutputStream output, Operation getFeature)
			throws IOException, ServiceException {
		Date date_getRequest = new Date();
		ByteArrayOutputStream gmlOutputStream = new ByteArrayOutputStream();

		super.write(featureCollection, gmlOutputStream, getFeature);
//		System.out.println(gmlOutputStream.toString("UTF-8"));
		
		byte[] inputBytes = gmlOutputStream.toByteArray();
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
	}
	
	@Override
	public String getMimeType(Object value, Operation operation) {
		return "application.gml3.lzma/zip; subtype=gml/3.1.1";
	}

}