package org.geoserver.wfs.format.ext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureTypeImplExt;
import org.geoserver.wfs.xml.GML3OutputFormat;
import org.geoserver.wfs.xml.v1_1_0.WFSConfiguration;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.siemens.ct.exi.CodingMode;
import com.siemens.ct.exi.EXIFactory;
import com.siemens.ct.exi.api.sax.EXIResult;
import com.siemens.ct.exi.helpers.DefaultEXIFactory;

public class GML3EXIOutputFormat extends GML3OutputFormat {

	public GML3EXIOutputFormat(GeoServer geoServer, WFSConfiguration configuration) {
		super(new HashSet(Arrays.asList(new Object[]{"gml3-javaexi", "application/xhtml+xml;"})), 
				geoServer, configuration);
	}
	
	@Override
	protected void write(FeatureCollectionResponse featureCollection, OutputStream output, Operation getFeature)
			throws IOException, ServiceException {		
		Date date_getRequest = new Date();
		ByteArrayOutputStream gmlOutputStream = new ByteArrayOutputStream();

		super.write(featureCollection, gmlOutputStream, getFeature);
		//System.out.println(gmlOutputStream.toString("UTF-8"));
		
		InputStream is = new ByteArrayInputStream(gmlOutputStream.toByteArray());
		InputSource inputSource = new InputSource(is);
		try {
			GetFeatureTypeImplExt implExt = (GetFeatureTypeImplExt) getFeature.getParameters()[0];
			String exiCodingModeString = implExt.getEXICodingMode().toUpperCase();
			CodingMode codingMode = CodingMode.BIT_PACKED;
			switch (exiCodingModeString) {
				case "BIT_PACKED":{
					codingMode = CodingMode.BIT_PACKED;
					break;
				}
				case "BYTE_PACKED":{
					codingMode = CodingMode.BYTE_PACKED;
					break;
				}
				case "COMPRESSION":{
					codingMode = CodingMode.COMPRESSION;
					break;
				}
				case "PRE_COMPRESSION":{
					codingMode = CodingMode.PRE_COMPRESSION;
					break;
				}
				default:{
					codingMode = CodingMode.BIT_PACKED;
				}
			}
			
			EXIFactory exiFactory = DefaultEXIFactory.newInstance();
			exiFactory.setCodingMode(codingMode);
			OutputStream osEXI = output;
			EXIResult exiResult = new EXIResult(exiFactory);
			exiResult.setOutputStream(osEXI);
			XMLReader xmlReader = XMLReaderFactory.createXMLReader();
			xmlReader.setContentHandler(exiResult.getHandler());
			xmlReader.parse(inputSource);
			osEXI.close();
		} catch (Exception ex) {
			throw new IOException(ex);
		}
		Date date_end = new Date();
	}
	
	@Override
	public String getMimeType(Object value, Operation operation) {
		return "application.gml3.exi/exi; subtype=gml/3.1.1";
	}

}