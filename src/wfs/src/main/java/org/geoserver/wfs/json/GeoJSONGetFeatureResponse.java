/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.json;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.xml.resolver.readers.TextCatalogReader;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.format.ext.TimeUsedForDataGeneralizingExport;
import org.geoserver.wfs.format.ext.TimeUsedForDataPreparingExport;
import org.geoserver.wfs.format.simplify.SimplifiedSimpleFeatureCollection;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureTypeImplExt;
import org.geotools.data.crs.ForceCoordinateSystemFeatureResults;
import org.geotools.data.crs.ReprojectFeatureResults;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.gml2.SrsSyntax;
import org.geotools.referencing.CRS;
import org.geotools.referencing.NamedIdentifier;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;

import net.sf.json.JSONException;

/**
 * A GetFeatureInfo response handler specialized in producing Json and JsonP data for a GetFeatureInfo request.
 * 
 * @author Simone Giannecchini, GeoSolutions
 * @author Carlo Cancellieri - GeoSolutions
 * 
 */
public class GeoJSONGetFeatureResponse extends WFSGetFeatureOutputFormat {
    private final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(this.getClass());

    // store the response type
    private final boolean jsonp;

    public GeoJSONGetFeatureResponse(GeoServer gs, String format) {
        super(gs, format);
        if (JSONType.isJsonMimeType(format)) {
            jsonp = false;
        } else if (JSONType.isJsonpMimeType(format)) {
            jsonp = true;
        } else if (JSONType.isJson2EXIMimeType(format)) {
        	jsonp = false;
        } else if (JSONType.isJson2ZipMimeType(format)) {
        	jsonp = false;
        }  else {
            throw new IllegalArgumentException(
                    "Unable to create the JSON Response handler using format: " + format
                            + " supported mymetype are: "
                            + Arrays.toString(JSONType.getSupportedTypes()));
        }
    }

    /**
     * capabilities output format string.
     */
    public String getCapabilitiesElementName() {
        return JSONType.getJSONType(getOutputFormat()).toString();
    }

    /**
     * Returns the mime type
     */
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        if(jsonp) {
            return JSONType.JSONP.getMimeType();
        } else {
            return JSONType.JSON.getMimeType();
        }
    }

    @Override
    protected void write(FeatureCollectionResponse featureCollection, OutputStream output,
            Operation describeFeatureType) throws IOException {
    	Date date_begin = new Date();

        int numDecimals = getNumDecimals(featureCollection.getFeature(), gs, gs.getCatalog());

        if (LOGGER.isLoggable(Level.INFO))
            LOGGER.info("about to encode JSON");
        // Generate bounds for every feature?
        WFSInfo wfs = getInfo();
        boolean featureBounding = wfs.isFeatureBounding();
        
        // include fid?
        String id_option = null; // null - default, "" - none, or "property"
        //GetFeatureRequest request = GetFeatureRequest.adapt(describeFeatureType.getParameters()[0]);
        Request request = Dispatcher.REQUEST.get();
        if (request != null) {
            id_option = JSONType.getIdPolicy( request.getKvp() );
        }
        // prepare to write out
        OutputStreamWriter osw = null;
        Writer outWriter = null; 
        boolean hasGeom = false;

        // get feature count for request
        // If the bbox is used, the result feature count will be changed here, which means the filter work is done before this step
        BigInteger totalNumberOfFeatures = featureCollection.getTotalNumberOfFeatures();
        BigInteger featureCount = (totalNumberOfFeatures != null && totalNumberOfFeatures.longValue() < 0)
                ? null : totalNumberOfFeatures;
        System.out.println("total feature count for Json collection is "+featureCount);
        
        try {
            osw = new OutputStreamWriter(output, gs.getGlobal().getSettings().getCharset());
            outWriter = new BufferedWriter(osw);

            if (jsonp) {
                outWriter.write(getCallbackFunction() + "(");
            }
            
            final GeoJSONBuilder jsonWriter = new GeoJSONBuilder(outWriter);
            jsonWriter.setNumberOfDecimals(numDecimals);
            jsonWriter.object().key("type").value("FeatureCollection");
            if(featureCount != null) {
                jsonWriter.key("totalFeatures").value(featureCount);
            } else {
                jsonWriter.key("totalFeatures").value("unknown");
            }
            jsonWriter.key("features");
            jsonWriter.array();
            
            
//          before encoding, execute the simplify method here
            GetFeatureTypeImplExt implExt = (GetFeatureTypeImplExt) describeFeatureType.getParameters()[0];
            List<FeatureCollection> featureCollections = featureCollection.getFeature();
            if(implExt.getSimplifyMethod()!=GetFeatureTypeImplExt.SIMPLIFYMETHOD_NONE){
    			System.out.println("Conduct geometry simplify in JSON, "+implExt.getSimplifyMethod()+", "+implExt.getSimplifyDistanceTolerance()+"");
    			long currentTime_beginSimplify = System.currentTimeMillis();
    			int totalSize = 0;
    	        int totalSizeSimple = 0;
    			ArrayList<SimpleFeatureCollection>featureCollectionsBuffer = new ArrayList<>();
        		if(implExt.getSimplifyMethod().equalsIgnoreCase(GetFeatureTypeImplExt.SIMPLIFYMETHOD_DP)){
                	double distanceTolerance = implExt.getSimplifyDistanceTolerance();
                	for(int i=0; i<featureCollections.size(); i++){
                  	   if(featureCollections.get(i) instanceof SimpleFeatureCollection){  // could be ReprojectFeatureResults or ForceCoordinateSystemFeatureResults 
                  		 SimpleFeatureCollection reprojectFeatureResults = (SimpleFeatureCollection) featureCollections.get(i);
                  		   SimplifiedSimpleFeatureCollection collectionNew = new SimplifiedSimpleFeatureCollection();
                  		   featureCollectionsBuffer.add(collectionNew);
                  		   collectionNew.setBounds(reprojectFeatureResults.getBounds());
                  		   collectionNew.setID(reprojectFeatureResults.getID());
                  		   collectionNew.setSchema(reprojectFeatureResults.getSchema());
                  		   SimpleFeatureIterator features = reprojectFeatureResults.features();
                  		   while(features.hasNext()){
                  			   SimpleFeature next = (SimpleFeature) features.next();
                  			   Geometry defaultGeometry = (Geometry) next.getDefaultGeometry();
                  			   totalSize += defaultGeometry.getNumPoints();
                			   Geometry simplify = DouglasPeuckerSimplifier.simplify(defaultGeometry, distanceTolerance);
                			   next.setDefaultGeometry(simplify);
                			   collectionNew.addSimpleFeature(next);
                  		   }
                  	   }else{
                  		   System.err.println("Unsupported data type!! " + (featureCollections.get(i).getClass().getName()));
                  	   }
                     }
                }
                else if (implExt.getSimplifyMethod().equalsIgnoreCase(GetFeatureTypeImplExt.SIMPLIFYMETHOD_TP)) {
                	double distanceTolerance = implExt.getSimplifyDistanceTolerance();
                	for(int i=0; i<featureCollections.size(); i++){
                  	   if(featureCollections.get(i) instanceof ReprojectFeatureResults){
                  		 ReprojectFeatureResults reprojectFeatureResults = (ReprojectFeatureResults) featureCollections.get(i);
                		   SimplifiedSimpleFeatureCollection collectionNew = new SimplifiedSimpleFeatureCollection();
                		   featureCollectionsBuffer.add(collectionNew);
                		   collectionNew.setBounds(reprojectFeatureResults.getBounds());
                		   collectionNew.setID(reprojectFeatureResults.getID());
                		   collectionNew.setSchema(reprojectFeatureResults.getSchema());
                		   SimpleFeatureIterator features = reprojectFeatureResults.features();
                  		   while(features.hasNext()){
                  			   SimpleFeature next = features.next();
                  			   Geometry defaultGeometry = (Geometry) next.getDefaultGeometry();
                  			   totalSize += defaultGeometry.getNumPoints();
                  			   Geometry simplify = TopologyPreservingSimplifier.simplify(defaultGeometry, distanceTolerance);
                  			   next.setDefaultGeometry(simplify);
                  			   collectionNew.addSimpleFeature(next);
                  		   }
                  	   }
                     }
        		}
        		featureCollection.getFeature().clear();
        		featureCollection.getFeature().addAll(featureCollectionsBuffer);
        		

    	        int totalFeatureCount = 0;
                featureCollections = featureCollection.getFeature(); 
                for (int i = 0; i < featureCollections.size(); i++) {
        			if (featureCollections.get(i) instanceof SimplifiedSimpleFeatureCollection) {
        				SimplifiedSimpleFeatureCollection reprojectFeatureResults = (SimplifiedSimpleFeatureCollection) featureCollections.get(i);
        				SimpleFeatureIterator features = reprojectFeatureResults.features();
        				while (features.hasNext()) {
        					SimpleFeature next = features.next();
        					Geometry defaultGeometry = (Geometry) next.getDefaultGeometry();
        					totalSizeSimple += defaultGeometry.getNumPoints();
                 			totalFeatureCount += 1;
        				}
        			}
        		}
        		System.out.println(String.format("total feature count: %d; Result point count: origin-%d, simplified-%d", totalFeatureCount, totalSize, totalSizeSimple));
        		TimeUsedForDataGeneralizingExport.timeUsedForDataGeneralizing = (int)(System.currentTimeMillis()-currentTime_beginSimplify);
        	}

            // execute should of set all the header information
            // including the lockID
            //
            // execute should also fail if all of the locks could not be acquired
            List<FeatureCollection> resultsList = featureCollection.getFeature();
            CoordinateReferenceSystem crs = null;
            for (int i = 0; i < resultsList.size(); i++) {
                FeatureCollection collection = resultsList.get(i);
                FeatureIterator iterator = collection.features();

                
                try {
                    SimpleFeatureType fType;
                    List<AttributeDescriptor> types;

                    while (iterator.hasNext()) {
                        SimpleFeature feature = (SimpleFeature) iterator.next();
                        jsonWriter.object();
                        jsonWriter.key("type").value("Feature");

                        fType = feature.getFeatureType();
                        types = fType.getAttributeDescriptors();

                        if( id_option == null ){
                            jsonWriter.key("id").value(feature.getID());
                        }
                        else if ( id_option.length() != 0){
                            Object value = feature.getAttribute(id_option);
                            jsonWriter.key("id").value(value);
                        }
                        
                        GeometryDescriptor defaultGeomType = fType.getGeometryDescriptor();
                        if(defaultGeomType != null) {
                            CoordinateReferenceSystem featureCrs =
                                    defaultGeomType.getCoordinateReferenceSystem();
                            
                            jsonWriter.setAxisOrder(CRS.getAxisOrder(featureCrs));
                            
                            if (crs == null)
                                crs = featureCrs;
                        } else  {
                            // If we don't know, assume EAST_NORTH so that no swapping occurs
                            jsonWriter.setAxisOrder(CRS.AxisOrder.EAST_NORTH);
                        }
                        
                        jsonWriter.key("geometry");
                        Geometry aGeom = (Geometry) feature.getDefaultGeometry();

                        // Write the geometry, whether it is a null or not
                        if (aGeom != null) {
                            jsonWriter.writeGeom(aGeom);
                            hasGeom = true;
                        } else {
                            jsonWriter.value(null);
                        }
                        if (defaultGeomType != null)
                            jsonWriter.key("geometry_name").value(defaultGeomType.getLocalName());

                        jsonWriter.key("properties");
                        jsonWriter.object();

                        for (int j = 0; j < types.size(); j++) {
                            Object value = feature.getAttribute(j);
                            AttributeDescriptor ad = types.get(j);
                            
                            if( id_option != null && id_option.equals(ad.getLocalName()) ){
                            	continue; // skip this value as it is used as the id
                            }
                            if (ad instanceof GeometryDescriptor) {
                                // This is an area of the spec where they
                                // decided to 'let convention evolve',
                                // that is how to handle multiple
                                // geometries. My take is to print the
                                // geometry here if it's not the default.
                                // If it's the default that you already
                                // printed above, so you don't need it here.
                                if (ad.equals(defaultGeomType)) {
                                    // Do nothing, we wrote it above
                                    // jsonWriter.value("geometry_name");
                                } else if(value == null){
                                    jsonWriter.key(ad.getLocalName());
                                    jsonWriter.value(null);
                                } else {
                                    jsonWriter.key(ad.getLocalName());
                                    jsonWriter.writeGeom((Geometry) value);
                                }
                            } else {
                                jsonWriter.key(ad.getLocalName());
                                jsonWriter.value(value);
                            }
                        }
                        // Bounding box for feature in properties
                        ReferencedEnvelope refenv = ReferencedEnvelope.reference(feature.getBounds());
                        if (featureBounding && !refenv.isEmpty())
                            jsonWriter.writeBoundingBox(refenv);

                        jsonWriter.endObject(); // end the properties
                        jsonWriter.endObject(); // end the feature
                    }
                } // catch an exception here?
                finally {
                    iterator.close();
                }
            }
            jsonWriter.endArray(); // end features

            // Coordinate Reference System
            try {
                if ("true".equals(GeoServerExtensions.getProperty("GEOSERVER_GEOJSON_LEGACY_CRS"))){
                    // This is wrong, but GeoServer used to do it this way.
                    writeCrsLegacy(jsonWriter, crs);
                } else {
                    writeCrs(jsonWriter, crs);
                }
            } catch (FactoryException e) {
                throw (IOException) new IOException("Error looking up crs identifier").initCause(e);
            }
            
            // Bounding box for featurecollection
            if (hasGeom && featureBounding) {
                ReferencedEnvelope e = null;
                for (int i = 0; i < resultsList.size(); i++) {
                    FeatureCollection collection = resultsList.get(i);
                    if (e == null) {
                        e = collection.getBounds();
                    } else {
                        e.expandToInclude(collection.getBounds());
                    }

                }

                if (e != null) {
                    jsonWriter.setAxisOrder(CRS.getAxisOrder(e.getCoordinateReferenceSystem()));
                    jsonWriter.writeBoundingBox(e);
                }
            }

            jsonWriter.endObject(); // end featurecollection

            if (jsonp) {
                outWriter.write(")");
            }

            outWriter.flush();

        } catch (JSONException jsonException) {
            ServiceException serviceException = new ServiceException("Error: "
                    + jsonException.getMessage());
            serviceException.initCause(jsonException);
            throw serviceException;
        }
        Date date_end = new Date();
        System.out.println("Time for GeoJson preparing = "+(date_end.getTime() - date_begin.getTime()));
        TimeUsedForDataPreparingExport.timeUsedForDataPreparing = (int)(date_end.getTime()-date_begin.getTime());
    }

    private void writeCrs(final GeoJSONBuilder jsonWriter,
            CoordinateReferenceSystem crs) throws FactoryException {
        if (crs != null) {
            String identifier = null;
            Integer code = CRS.lookupEpsgCode(crs, true);
            if(code != null) {
                if (code != null) {
                    identifier = SrsSyntax.OGC_URN.getPrefix() + code;
                }
            } else {
                identifier = CRS.lookupIdentifier(crs, true);
            }
            
            jsonWriter.key("crs");
            jsonWriter.object();
            jsonWriter.key("type").value("name");
            jsonWriter.key("properties");
            jsonWriter.object();
            jsonWriter.key("name");
            jsonWriter.value(identifier);
            jsonWriter.endObject(); // end properties
            jsonWriter.endObject(); // end crs
        } else {
            jsonWriter.key("crs");
            jsonWriter.value(null);
        }
    }
    
    // Doesn't follow spec, but GeoServer used to do this.
    private void writeCrsLegacy(final GeoJSONBuilder jsonWriter,
            CoordinateReferenceSystem crs) {
        // Coordinate Reference System, currently only if the namespace is
        // EPSG
        if (crs != null) {
            Set<ReferenceIdentifier> ids = crs.getIdentifiers();
            // WKT defined crs might not have identifiers at all
            if (ids != null && ids.size() > 0) {
                NamedIdentifier namedIdent = (NamedIdentifier) ids.iterator().next();
                String csStr = namedIdent.getCodeSpace().toUpperCase();

                if (csStr.equals("EPSG")) {
                    jsonWriter.key("crs");
                    jsonWriter.object();
                    jsonWriter.key("type").value(csStr);
                    jsonWriter.key("properties");
                    jsonWriter.object();
                    jsonWriter.key("code");
                    jsonWriter.value(namedIdent.getCode());
                    jsonWriter.endObject(); // end properties
                    jsonWriter.endObject(); // end crs
                }
            }
        }
    }

    private String getCallbackFunction() {
        Request request = Dispatcher.REQUEST.get();
        if (request == null) {
            return JSONType.CALLBACK_FUNCTION;
        }
        return JSONType.getCallbackFunction(request.getKvp());
    }

    
    @Override
    public String getCharset(Operation operation){
        return gs.getGlobal().getSettings().getCharset();
    }
}
