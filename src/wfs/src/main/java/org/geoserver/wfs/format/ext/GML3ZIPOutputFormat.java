package org.geoserver.wfs.format.ext;

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


public class GML3ZIPOutputFormat extends GML3OutputFormat {

	public GML3ZIPOutputFormat(GeoServer geoServer, WFSConfiguration configuration) {
		super(new HashSet(Arrays.asList(new Object[]{"gml3-zip", "application/xhtml+xml;"})), 
				geoServer, configuration);
	}
	
	@Override
	protected void write(FeatureCollectionResponse featureCollection, OutputStream output, Operation getFeature)
			throws IOException, ServiceException {
		Date date_getRequest = new Date();
		ByteArrayOutputStream gmlOutputStream = new ByteArrayOutputStream();

		super.write(featureCollection, gmlOutputStream, getFeature);
//		System.out.println(gmlOutputStream.toString("UTF-8"));
	
////		can successfully export .zip file
//		ZipOutputStream zos = new ZipOutputStream(output);
//		ZipEntry ze = new ZipEntry("output.gml");
//		zos.putNextEntry(ze);
//		zos.write(gmlOutputStream.toByteArray());
//		zos.closeEntry();
//		zos.finish();
//		zos.close();
		
//	can successfully export .gz file
        GZIPOutputStream gzip = new GZIPOutputStream(output);
        gzip.write(gmlOutputStream.toByteArray());
        gzip.close();
        Date date_end = new Date();
	}
	
	@Override
	public String getMimeType(Object value, Operation operation) {
		return "application.gml3.gz/zip; subtype=gml/3.1.1";
	}

}