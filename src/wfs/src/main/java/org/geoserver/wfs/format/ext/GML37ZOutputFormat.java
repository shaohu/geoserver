package org.geoserver.wfs.format.ext;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
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
import org.tukaani.xz.FilterOptions;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;
import org.geoserver.wfs.format.ext.SevenZip.Compression.LZMA.Encoder;

public class GML37ZOutputFormat extends GML3OutputFormat {

	public GML37ZOutputFormat(GeoServer geoServer, WFSConfiguration configuration) {
		super(new HashSet(Arrays.asList(new Object[]{"gml3-7z", "application/xhtml+xml;"})), 
				geoServer, configuration);
	}
	
	@Override
	protected void write(FeatureCollectionResponse featureCollection, OutputStream output, Operation getFeature)
			throws IOException, ServiceException {		
		ByteArrayOutputStream gmlOutputStream = new ByteArrayOutputStream();

		super.write(featureCollection, gmlOutputStream, getFeature);
//		System.out.println(gmlOutputStream.toString("UTF-8"));
        
		LZMA2Options options = new LZMA2Options();
		XZOutputStream xzout = new XZOutputStream(output, options);
		InputStream is = new ByteArrayInputStream(gmlOutputStream.toByteArray());

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
	}
	
	@Override
	public String getMimeType(Object value, Operation operation) {
		return "application.gml3.lzma/zip; subtype=gml/3.1.1";
	}

}