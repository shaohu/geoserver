package org.geoserver.wfs.kvp;

import net.opengis.wfs.impl.GetFeatureTypeImpl;

public class GetFeatureTypeImplExt extends GetFeatureTypeImpl {
	protected static final String SIMPILIFYMETHOD_DEFAULT = "NONE";
	protected String simpilifyMethod = SIMPILIFYMETHOD_DEFAULT;
	
	public String getSimpifyMethod() {
        return this.simpilifyMethod;
    }

	/**
     * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
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
