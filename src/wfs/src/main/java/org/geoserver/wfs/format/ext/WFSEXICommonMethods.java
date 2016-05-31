package org.geoserver.wfs.format.ext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.xerces.util.EntityResolver2Wrapper;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;

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
}
