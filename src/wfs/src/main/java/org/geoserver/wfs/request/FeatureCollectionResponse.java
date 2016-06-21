/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.request;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.WfsFactory;
import net.opengis.wfs20.Wfs20Factory;

import org.eclipse.emf.ecore.EObject;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.kvp.GetFeatureTypeImplExt;
import org.geotools.data.crs.ReprojectFeatureResults;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;

/**
 * Response object for a feature collection, most notably from a GetFeature request.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class FeatureCollectionResponse extends RequestObject {

    public static FeatureCollectionResponse adapt(Object adaptee) {
        if (adaptee instanceof FeatureCollectionType) {
            return new WFS11((EObject) adaptee);
        }
        else if (adaptee instanceof net.opengis.wfs20.FeatureCollectionType) {
            return new WFS20((EObject) adaptee);
        }
        return null;
    }

    protected FeatureCollectionResponse(EObject adaptee) {
        super(adaptee);
    }

    public String getLockId() {
        return eGet(adaptee, "lockId", String.class);
    }
    public void setLockId(String lockId) {
        eSet(adaptee, "lockId", lockId);
    }

    public Calendar getTimeStamp() {
        return eGet(adaptee, "timeStamp", Calendar.class);
    }
    public void setTimeStamp(Calendar timeStamp) {
        eSet(adaptee, "timeStamp", timeStamp);
    }

    public abstract FeatureCollectionResponse create();

    public abstract BigInteger getNumberOfFeatures();
    public abstract void setNumberOfFeatures(BigInteger n);

    public abstract BigInteger getTotalNumberOfFeatures();
    public abstract void setTotalNumberOfFeatures(BigInteger n);

    public abstract void setPrevious(String previous);
    public abstract String getPrevious();

    public abstract void setNext(String next);
    public abstract String getNext();
    
    public abstract List<FeatureCollection> getFeatures();
    
    public abstract Object unadapt(Class target);

    public List<FeatureCollection> getFeature() {
        //alias
    	int totalSizeSimple = 0;
    	int totalSize = 0;
    	List featureCollections = getFeatures();
    	if(this.getSimplifyMethod()!=GetFeatureTypeImplExt.SIMPILIFYMETHOD_NONE){
    		if(this.simplifiedFeatureCollections!=null){
        		System.out.println("feature simplify request detected, use existing one +++++++++++++++++++++");
    			return this.simplifiedFeatureCollections;
    		}
    		else{
    			System.out.println("need to deal with simplify, "+this.getSimplifyMethod()+" - "+this.getSimplifyDistanceTolerance()+"++++++++++");
        		if(this.getSimplifyMethod().equalsIgnoreCase(GetFeatureTypeImplExt.SIMPILIFYMETHOD_DP))
                {
                	double distanceTolerance = this.getSimplifyDistanceTolerance();
                	for(int i=0; i<featureCollections.size(); i++){
                  	   if(featureCollections.get(i) instanceof org.geotools.data.crs.ReprojectFeatureResults){
                  		   org.geotools.data.crs.ReprojectFeatureResults reprojectFeatureResults = (ReprojectFeatureResults) featureCollections.get(i);
//                  		   FeatureCollection<SimpleFeatureType, SimpleFeature> origin = reprojectFeatureResults.getOrigin();
//                  		   SimpleFeatureIterator features = reprojectFeatureResults.features();
                  		 Iterator<SimpleFeature> features = reprojectFeatureResults.iterator();
                  		   while(features.hasNext()){
                  			   SimpleFeature next = features.next();
                  			   Geometry defaultGeometry = (Geometry) next.getDefaultGeometry();
                			   Geometry simplify = DouglasPeuckerSimplifier.simplify(defaultGeometry, distanceTolerance);
                			   next.setDefaultGeometry(simplify);
                			   totalSizeSimple += ((Geometry) next.getDefaultGeometry()).getNumPoints();
//                			   System.out.println("DP simplify tolerance = " + distanceTolerance+ "-" + defaultGeometry.getNumPoints() + "-" + ((Geometry)next.getDefaultGeometry()).getNumPoints());
                  		   }
                  	   }
                     }
                }
                else if (this.getSimplifyMethod().equalsIgnoreCase(GetFeatureTypeImplExt.SIMPILIFYMETHOD_TP)) {
                	double distanceTolerance = this.getSimplifyDistanceTolerance();
                	for(int i=0; i<featureCollections.size(); i++){
                  	   if(featureCollections.get(i) instanceof org.geotools.data.crs.ReprojectFeatureResults){
                  		   org.geotools.data.crs.ReprojectFeatureResults reprojectFeatureResults = (ReprojectFeatureResults) featureCollections.get(i);
                  		   SimpleFeatureIterator features = reprojectFeatureResults.features();
                  		   while(features.hasNext()){
                  			   SimpleFeature next = features.next();
                  			   Geometry defaultGeometry = (Geometry) next.getDefaultGeometry();
                  			   Geometry simplify = TopologyPreservingSimplifier.simplify(defaultGeometry, distanceTolerance);
                  			   next.setDefaultGeometry(simplify);
                			   totalSizeSimple += ((Geometry) next.getDefaultGeometry()).getNumPoints();
//                  			   System.out.println("TP simplify tolerance = " + distanceTolerance+ "-" + defaultGeometry.getNumPoints() + "-" + ((Geometry)next.getDefaultGeometry()).getNumPoints());
                  		   }
                  	   }
                     }
        		}
        		this.simplifiedFeatureCollections = featureCollections;
    		}
    	}
    	
    	
    	
    	
		for (int i = 0; i < featureCollections.size(); i++) {
			if (featureCollections.get(i) instanceof org.geotools.data.crs.ReprojectFeatureResults) {
				org.geotools.data.crs.ReprojectFeatureResults reprojectFeatureResults = (ReprojectFeatureResults) featureCollections
						.get(i);
				Iterator<SimpleFeature> features = reprojectFeatureResults.iterator();
				while (features.hasNext()) {
					SimpleFeature next = features.next();
					Geometry defaultGeometry = (Geometry) next.getDefaultGeometry();
					totalSize += defaultGeometry.getNumPoints();
				}
			}
		}
		
		System.out.println("result point count ============= "+totalSize + "'''''"+ totalSizeSimple);
		
    	
        return featureCollections;
    }
    
    protected String simplifyMethod = GetFeatureTypeImplExt.SIMPILIFYMETHOD_NONE;
    protected double simplifyDistanceTolerance = 0;
    protected List simplifiedFeatureCollections = null; 
    
    public String getSimplifyMethod(){
    	return this.simplifyMethod;
    }
    
    public void setSimplifyMethod(String simplifyMethod) {
		this.simplifyMethod = simplifyMethod;
	}
    
    public double getSimplifyDistanceTolerance(){
    	return this.simplifyDistanceTolerance;
    }
    
    public void setSimplifyDistanceTolerance(double simplifyDistanceTolerance) {
		this.simplifyDistanceTolerance = simplifyDistanceTolerance;
	}

    public static class WFS11 extends FeatureCollectionResponse {
        BigInteger totalNumberOfFeatures;
        
        public WFS11(EObject adaptee) {
            super(adaptee);
        }

        @Override
        public FeatureCollectionResponse create() {
            return FeatureCollectionResponse.adapt(((WfsFactory)getFactory()).createFeatureCollectionType());
        }

        @Override
        public BigInteger getNumberOfFeatures() {
            return eGet(adaptee, "numberOfFeatures", BigInteger.class);
        }
        @Override
        public void setNumberOfFeatures(BigInteger n) {
            eSet(adaptee, "numberOfFeatures", n);
        }

        @Override
        public BigInteger getTotalNumberOfFeatures() {
            return totalNumberOfFeatures;
        }
        @Override
        public void setTotalNumberOfFeatures(BigInteger n) {
            this.totalNumberOfFeatures = n;
        }

        @Override
        public String getPrevious() {
            //noop
            return null;
        }

        @Override
        public void setPrevious(String previous) {
            //noop
        }

        @Override
        public String getNext() {
            //noop
            return null;
        }

        @Override
        public void setNext(String next) {
            //noop
        }

        @Override
        public List<FeatureCollection> getFeatures() {
            return eGet(adaptee, "feature", List.class);
        }

        @Override
        public Object unadapt(Class target) {
            if (target.equals(FeatureCollectionType.class)) {
                return adaptee;
            } else if (target.equals(net.opengis.wfs20.FeatureCollectionType.class)) {
                FeatureCollectionType source = (FeatureCollectionType) adaptee;
                net.opengis.wfs20.FeatureCollectionType result = Wfs20Factory.eINSTANCE
                        .createFeatureCollectionType();
                result.getMember().addAll(source.getFeature());
                result.setNumberReturned(source.getNumberOfFeatures());
                result.setLockId(source.getLockId());
                result.setTimeStamp(source.getTimeStamp());
                return result;
            } else {
                throw new WFSException("Cannot transform " + adaptee
                        + " to the specified target class " + target);
            }
        }
    }

    public static class WFS20 extends FeatureCollectionResponse {
        public WFS20(EObject adaptee) {
            super(adaptee);
        }

        @Override
        public FeatureCollectionResponse create() {
            return FeatureCollectionResponse.adapt(((Wfs20Factory)getFactory()).createFeatureCollectionType());
        }

        @Override
        public BigInteger getNumberOfFeatures() {
            return eGet(adaptee, "numberReturned", BigInteger.class);
        }

        @Override
        public void setNumberOfFeatures(BigInteger n) {
            eSet(adaptee, "numberReturned", n);
        }

        @Override
        public BigInteger getTotalNumberOfFeatures() {
            return eGet(adaptee, "numberMatched", BigInteger.class);
        }
        @Override
        public void setTotalNumberOfFeatures(BigInteger n) {
            eSet(adaptee, "numberMatched", (n.longValue() < 0) ? null : n);
        }

        @Override
        public String getPrevious() {
            return eGet(adaptee, "previous", String.class);
        }

        @Override
        public void setPrevious(String previous) {
            eSet(adaptee, "previous", previous);
        }

        @Override
        public String getNext() {
            return eGet(adaptee, "next", String.class);
        }

        @Override
        public void setNext(String next) {
            eSet(adaptee, "next", next);
        }

        @Override
        public List<FeatureCollection> getFeatures() {
            return eGet(adaptee, "member", List.class);
        }

        @Override
        public Object unadapt(Class target) {
            if (target.equals(net.opengis.wfs20.FeatureCollectionType.class)) {
                return adaptee;
            } else if (target.equals(FeatureCollectionType.class)) {
                net.opengis.wfs20.FeatureCollectionType source = (net.opengis.wfs20.FeatureCollectionType) adaptee;
                FeatureCollectionType result = WfsFactory.eINSTANCE.createFeatureCollectionType();
                result.getFeature().addAll(source.getMember());
                result.setNumberOfFeatures(source.getNumberReturned());
                result.setLockId(source.getLockId());
                result.setTimeStamp(source.getTimeStamp());
                return result;
            } else {
                throw new WFSException("Cannot transform " + adaptee
                        + " to the specified target class " + target);
            }
        }
    }

}
