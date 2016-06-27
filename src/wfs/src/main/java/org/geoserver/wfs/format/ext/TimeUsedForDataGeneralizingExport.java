package org.geoserver.wfs.format.ext;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.xml.GML3OutputFormat;
import org.geoserver.wfs.xml.v1_1_0.WFSConfiguration;

public class TimeUsedForDataGeneralizingExport extends GML3OutputFormat {
	static public int timeUsedForDataGeneralizing = 0;
	public TimeUsedForDataGeneralizingExport(GeoServer geoServer, WFSConfiguration configuration){
		super(new HashSet(Arrays.asList(new Object[]{"time-for-data-generalizing-int-text", "application/text;"})), 
				geoServer, configuration);
	}
	
	@Override
	protected void write(FeatureCollectionResponse featureCollection, OutputStream output, Operation getFeature)
			throws IOException, ServiceException {
		output.write(String.valueOf(timeUsedForDataGeneralizing).getBytes());
		timeUsedForDataGeneralizing = 0;
	}
	
	@Override
	public String getMimeType(Object value, Operation operation) {
		return "application.time.for.preparing.int/text; subtype=text";
	}
}
