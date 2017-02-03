package org.geoserver.wfs.format.ext;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.xml.GML3OutputFormat;
import org.geoserver.wfs.xml.v1_1_0.WFSConfiguration;
import org.tukaani.xz.FilterOptions;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;

public class TimeUsedForDataPreparingExport extends GML3OutputFormat{
	static public long timeGetWFSRequest = 0;
	static public long timeBeginGeneralization = 0;
	static public long timeBeginDataTransmission = 0;
	
	static public int totalPointsCount = 0;
	static public int totalPointsSimplifiedCount = 0;
	
	
	
	public TimeUsedForDataPreparingExport(GeoServer geoServer, WFSConfiguration configuration){
		super(new HashSet(Arrays.asList(new Object[]{"time-for-data-preparing-int-text", "application/text;"})), 
				geoServer, configuration);
	}
	
	@Override
	protected void write(FeatureCollectionResponse featureCollection, OutputStream output, Operation getFeature)
			throws IOException, ServiceException {
		
		String line = String.format(""
				+ "totalPointsCount:%s;"
				+ "totalPointsSimplifiedCount:%s;"
				+ "timeGetWFSRequest:%s;"
				+ "timeBeginGeneralization:%s;"
				+ "timeBeginDataTransmission:%s", 
				totalPointsCount, totalPointsSimplifiedCount, timeGetWFSRequest, timeBeginGeneralization, timeBeginDataTransmission);
		
		output.write(line.getBytes());
		timeGetWFSRequest = 0;
		timeBeginGeneralization = 0;
		timeBeginDataTransmission = 0;
		totalPointsCount = 0;
		totalPointsSimplifiedCount = 0;
	}
	
	@Override
	public String getMimeType(Object value, Operation operation) {
		return "application.time.for.preparing.int/text; subtype=text";
	}
}
