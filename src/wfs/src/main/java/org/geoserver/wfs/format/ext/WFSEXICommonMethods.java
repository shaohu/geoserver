package org.geoserver.wfs.format.ext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.xerces.util.EntityResolver2Wrapper;
import org.geoserver.wfs.format.simplify.SimplifiedSimpleFeatureCollection;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureTypeImplExt;
import org.geotools.data.crs.ForceCoordinateSystemFeatureResults;
import org.geotools.data.gen.PreGeneralizedSimpleFeature;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureImpl;
import org.geotools.feature.simple.SimpleFeatureTypeImpl;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;

public class WFSEXICommonMethods {
	
	/**
	 * Extract common strings from the collection, these common strings can be used for compressing
	 * @param featureCollection
	 * @return
	 */
	static public ArrayList<String> extractCommonStringsFromFeatureCollection(FeatureCollectionResponse featureCollection) {
		ArrayList<String> resultList = new ArrayList<String>();
		resultList.addAll(Arrays.asList("type", "totalFeatures", "features", "id", "geometry", "coordinates", "geometry_name", "properties"));
		
		Iterator<FeatureCollection> iterator = featureCollection.getFeature().iterator();
		if(iterator.hasNext()){
			FeatureCollection next = iterator.next();
			Feature next2 = next.features().next();
			Collection<Property> properties = next2.getProperties();
			Iterator<Property> propertiesIterator = properties.iterator();
			while(propertiesIterator.hasNext()){
				resultList.add(propertiesIterator.next().getName().toString());
			}
		}
		
		return resultList;
	}
	
	/**
	 * No matter the featureCollection comes from original or pregeneralized data set, 
	 * they should be processed and wrapped into ForceCoordinateSystemFeatureResults here in order to be correctlly processed later
	 * @param featureCollection
	 * @param request
	 * @return
	 */
	public static void generalize_feature_realtime(FeatureCollectionResponse result, GetFeatureTypeImplExt request) {
		List<FeatureCollection> features = result.getFeatures();
		System.out.println(features.get(0));
		if (request.getSimplifyMethod().equalsIgnoreCase(GetFeatureTypeImplExt.SIMPLIFYMETHOD_NONE)
				&& features.size()>0 && (features.get(0) instanceof ForceCoordinateSystemFeatureResults)) {
			return;
		}
		boolean isDP = true;
		if (request.getSimplifyMethod().equalsIgnoreCase(GetFeatureTypeImplExt.SIMPLIFYMETHOD_TP)) {
			isDP = false;
		}
		double distanceTolerance = request.getSimplifyDistanceTolerance();
		System.out.println(String.format(
				"Conduct the realtime geometry generalization in org.geoserver.wfs.GetFeature.generalize_feature_realtime. simplify method: %s; distance tolerance: %f",
				request.getSimplifyMethod(), distanceTolerance));
		ArrayList<SimpleFeatureCollection>featureCollectionsBuffer = new ArrayList<>();
		int totalSize = 0;
		int totalSizeSimplified = 0;
		
		for(int i=0; i<features.size(); i++){
			SimplifiedSimpleFeatureCollection collectionNew = new SimplifiedSimpleFeatureCollection();
			featureCollectionsBuffer.add(collectionNew);
			FeatureCollection featureCollection = features.get(i);
//			System.out.println(featureCollection.getSchema());
//			System.out.println(((ForceCoordinateSystemFeatureResults)featureCollection).getOrigin().getSchema());
//			System.out.println(((ForceCoordinateSystemFeatureResults)featureCollection).getOrigin());
			collectionNew.setBounds(featureCollection.getBounds());
			collectionNew.setID(featureCollection.getID());
			collectionNew.setSchema((SimpleFeatureType) featureCollection.getSchema());
			System.out.println("here is the schema of newly generalized data collection");
			System.out.println(featureCollection.getSchema());
			FeatureIterator<? extends Feature> iterator = featureCollection.features();
			SimpleFeatureType newSimpleFeatureType = null;
			while (iterator.hasNext()) {
				Feature nextFeature = iterator.next();
				if (nextFeature instanceof PreGeneralizedSimpleFeature) {
					// The pregeneralized feature was set unchangeable, so we can
					// not directly modify the geometry in each feature obj.
					// In order to do this, we need to build another new
					// SimpleFeatureImpl and copy all info inside of the
					// PreGeneralizedSimpleFeature to it
					// Here is the code of initializing a
					// PreGeneralizedSimpleFeature:
					// https://github.com/boundlessgeo/geotools-2.7.x/blob/master/modules/plugin/feature-pregeneralized/src/main/java/org/geotools/data/gen/PreGeneralizedSimpleFeature.java#L70

					/*
					 * public PreGeneralizedSimpleFeature(SimpleFeatureType
					 * featureTyp, int indexMapping[], SimpleFeature feature, String
					 * geomPropertyName, String backendGeomPropertyName) {
					 * 
					 * this.feature = feature; this.geomPropertyName =
					 * geomPropertyName; this.backendGeomPropertyName =
					 * backendGeomPropertyName; this.featureTyp = featureTyp;
					 * this.indexMapping = indexMapping;
					 * this.nameBackendGeomProperty = new
					 * NameImpl(backendGeomPropertyName);
					 * 
					 * }
					 */
					PreGeneralizedSimpleFeature generalizedSimpleFeature = (PreGeneralizedSimpleFeature) nextFeature;
					if(newSimpleFeatureType==null){
						SimpleFeatureType sft = generalizedSimpleFeature.getType();
//						AttributeDescriptor geomDescriptor = sft.getAttributeDescriptors().get(sft.getAttributeDescriptors().size()-1);
						ArrayList<AttributeDescriptor> attributeDescriptorList = new ArrayList<AttributeDescriptor>();
						attributeDescriptorList.add(sft.getGeometryDescriptor());
						if(generalizedSimpleFeature.getAttributes().size()==1){
							newSimpleFeatureType = new SimpleFeatureTypeImpl(sft.getName(), attributeDescriptorList, 
									sft.getGeometryDescriptor(),sft.isAbstract(), sft.getRestrictions(), sft.getSuper(), sft.getDescription());
						}else{
							newSimpleFeatureType = sft;
						}
						
					}
					
					SimpleFeatureImpl newFeature = new SimpleFeatureImpl(generalizedSimpleFeature.getAttributes(),
							newSimpleFeatureType, generalizedSimpleFeature.getIdentifier());
					newFeature.setDefaultGeometry(generalizedSimpleFeature.getDefaultGeometry());
					nextFeature = newFeature;
				}
				if (!(nextFeature instanceof SimpleFeatureImpl)) {
					System.err.println(
							"find unsupported feature type in org.geoserver.wfs.GetFeature.generalize_feature_realtime:"
									+ nextFeature.getClass().getName());
					return;
				}
				Geometry defaultGeometry = (Geometry) ((SimpleFeatureImpl) nextFeature).getDefaultGeometry();

				totalSize += defaultGeometry.getNumPoints();
				Geometry simplify = null;
				if(distanceTolerance==0.0){
					simplify = defaultGeometry;
				}else{
					if (isDP) {
						simplify = DouglasPeuckerSimplifier.simplify(defaultGeometry, distanceTolerance);
					} else {
						simplify = TopologyPreservingSimplifier.simplify(defaultGeometry, distanceTolerance);
					}
				}
				
				((SimpleFeatureImpl) nextFeature).setDefaultGeometry(simplify);
				totalSizeSimplified += simplify.getNumPoints();
				collectionNew.addSimpleFeature((SimpleFeatureImpl) nextFeature);
			}
			iterator.close();
          }
		result.getFeature().clear();
		result.getFeature().addAll(featureCollectionsBuffer);
		TimeUsedForDataPreparingExport.totalPointsCount = totalSize;
		TimeUsedForDataPreparingExport.totalPointsSimplifiedCount = totalSizeSimplified;
	}
}
