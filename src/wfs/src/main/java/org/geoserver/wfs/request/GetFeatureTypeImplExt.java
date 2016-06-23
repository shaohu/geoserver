package org.geoserver.wfs.request;

import net.opengis.wfs.impl.GetFeatureTypeImpl;

/**
 * An extension of net.opengis.wfs.impl.GetFeatureTypeImpl
 * All former variables and methods are preserved. While the new variable simplifyMethod could also be stored here. 
 * @author hushao
 *
 */
public class GetFeatureTypeImplExt extends GetFeatureTypeImpl {
	public static final String SIMPLIFYMETHOD_NONE = "NONE";
	/**
	 * the Douglas-Peucker algorithm
	 */
	public static final String SIMPLIFYMETHOD_DP = "DP";
	/**
	 * and topology-preserving simplification method 
	 */
	public static final String SIMPLIFYMETHOD_TP = "TP";
	protected String simplifyMethod = SIMPLIFYMETHOD_NONE;
	protected double simplifyDistanceTolerance = 0;
	
	public String getSimplifyMethod() {
        return this.simplifyMethod;
    }

	/**
	 * 
     * @generated
     */
	public void setSimplifyMethod(String simplifyMethod) {
        if(simplifyMethod.equalsIgnoreCase("DP")){
        	this.simplifyMethod = SIMPLIFYMETHOD_DP;
        }
        else if(simplifyMethod.equalsIgnoreCase("TP")){
        	this.simplifyMethod = SIMPLIFYMETHOD_TP;
        }
        else{
        	this.simplifyMethod = SIMPLIFYMETHOD_NONE;
        }
    }
	
	public double getSimplifyDistanceTolerance() {
		return this.simplifyDistanceTolerance;
	}
	
	public void setSimplifyDistanceTolerance(double value) {
		this.simplifyDistanceTolerance = value; 
	}
}
