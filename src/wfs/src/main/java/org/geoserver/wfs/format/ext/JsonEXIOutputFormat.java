package org.geoserver.wfs.format.ext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.json.GeoJSONGetFeatureResponse;
import org.geoserver.wfs.request.FeatureCollectionResponse;

import com.siemens.ct.exi.exceptions.EXIException;
import com.siemens.ct.exi.json.EXIforJSONGenerator;
import com.siemens.ct.exi.json.EXIforJSONParser;

public class JsonEXIOutputFormat extends GeoJSONGetFeatureResponse {

	public JsonEXIOutputFormat(GeoServer geoServer, String format) {
		super(geoServer, format);
	}

	protected void write(FeatureCollectionResponse featureCollection, OutputStream output,
			Operation getFeature) throws IOException {
		
		ByteArrayOutputStream jsonOutputStream = new ByteArrayOutputStream();
		super.write(featureCollection, jsonOutputStream, getFeature);
		//System.out.println(new String(jsonOutputStream.toByteArray()));
		InputStream jsonInputStream = new ByteArrayInputStream(jsonOutputStream.toByteArray());
		
		try {
			EXIforJSONGenerator e4jGenerator = new EXIforJSONGenerator();
			e4jGenerator.generate(jsonInputStream, output);
		} catch (EXIException ex) {
			throw new IOException(ex);
		}
	}
	
	@Override
	public String getMimeType(Object value, Operation operation) {
		return "applicationjson.exi/exi; subtype=json";
	}
}
