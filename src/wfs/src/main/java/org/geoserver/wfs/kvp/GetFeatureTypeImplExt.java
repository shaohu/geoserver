package org.geoserver.wfs.kvp;

import net.opengis.wfs.impl.GetFeatureTypeImpl;

/**
 * An extension of net.opengis.wfs.impl.GetFeatureTypeImpl
 * All former variables and methods are preserved. While the new variable simplifyMethod could also be stored here. 
 * @author hushao
 *
 */
public class GetFeatureTypeImplExt extends GetFeatureTypeImpl {
	protected static final String SIMPILIFYMETHOD_DEFAULT = "NONE";
	protected String simpilifyMethod = SIMPILIFYMETHOD_DEFAULT;
	
	public String getSimpifyMethod() {
        return this.simpilifyMethod;
    }

	/**
	 * 
     * @generated
     */
	public void setSimpifyMethod(String simpifyMethod) {
        if(simpifyMethod.equalsIgnoreCase("DP")){
        	this.simpilifyMethod = "DP";
        }
        else if(simpifyMethod.equalsIgnoreCase("TP")){
        	this.simpilifyMethod = "TP";
        }
        else{
        	this.simpilifyMethod = SIMPILIFYMETHOD_DEFAULT;
        }
    }
}
