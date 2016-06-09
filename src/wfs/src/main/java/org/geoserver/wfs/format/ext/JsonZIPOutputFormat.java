package org.geoserver.wfs.format.ext;

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

import com.siemens.ct.exi.exceptions.EXIException;
import com.siemens.ct.exi.json.EXIforJSONGenerator;
import com.siemens.ct.exi.json.EXIforJSONParser;

public class JsonZIPOutputFormat extends GeoJSONGetFeatureResponse {

	public JsonZIPOutputFormat(GeoServer geoServer, String format) {
		super(geoServer, format);
	}

	protected void write(FeatureCollectionResponse featureCollection, OutputStream output,
			Operation getFeature) throws IOException {
		Date date_getRequest = new Date();
		ByteArrayOutputStream jsonOutputStream = new ByteArrayOutputStream();
		super.write(featureCollection, jsonOutputStream, getFeature);
		//System.out.println(new String(jsonOutputStream.toByteArray()));
		
		Date date_begin = new Date();
        GZIPOutputStream gzip = new GZIPOutputStream(output);
        gzip.write(jsonOutputStream.toByteArray());
        gzip.close();
        Date date_end = new Date();
        System.out.println("Time for g-zip compression = "+(date_end.getTime() - date_begin.getTime()));
//        System.out.println("Size for g-zip compression = "+(jsonOutputStream.toByteArray().length));
        TimeUsedForDataPreparingExport.timeUsedForDataPreparing = (int)(date_end.getTime()-date_getRequest.getTime());
	}
	
	@Override
	public String getMimeType(Object value, Operation operation) {
		return "application.json.gz/zip; subtype=json";
	}
}
