package org.geoserver.wfs.kvp;

import net.opengis.wfs.impl.GetFeatureTypeImpl;

/**
 * An extension of net.opengis.wfs.impl.GetFeatureTypeImpl
 * All former variables and methods are preserved. While the new variable simplifyMethod could also be stored here. 
 * @author hushao
 *
 */
public class GetFeatureTypeImplExt extends GetFeatureTypeImpl {
	public static final String SIMPILIFYMETHOD_NONE = "NONE";
	/**
	 * the Douglas-Peucker algorithm
	 */
	public static final String SIMPILIFYMETHOD_DP = "DP";
	/**
	 * and topology-preserving simplification method 
	 */
	public static final String SIMPILIFYMETHOD_TP = "TP";
	protected String simpilifyMethod = SIMPILIFYMETHOD_NONE;
	protected double simpilifyDistanceTolerance = 0;
	
	public String getSimpifyMethod() {
        return this.simpilifyMethod;
    }

	/**
	 * 
     * @generated
     */
	public void setSimpifyMethod(String simpifyMethod) {
        if(simpifyMethod.equalsIgnoreCase("DP")){
        	this.simpilifyMethod = SIMPILIFYMETHOD_DP;
        }
        else if(simpifyMethod.equalsIgnoreCase("TP")){
        	this.simpilifyMethod = SIMPILIFYMETHOD_TP;
        }
        else{
        	this.simpilifyMethod = SIMPILIFYMETHOD_NONE;
        }
    }
	
	public double getSimpilifyDistanceTolerance() {
		return this.simpilifyDistanceTolerance;
	}
	
	public void setSimpilifyDistanceTolerance(double value) {
		this.simpilifyDistanceTolerance = value; 
	}
}
