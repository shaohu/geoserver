package org.geoserver.wfs.format.simplify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang.NotImplementedException;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.SimpleFeatureIteratorImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.omg.CORBA.Bounds;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.util.ProgressListener;

public class SimplifiedSimpleFeatureCollection implements SimpleFeatureCollection {


	protected String id = "";
	protected ReferencedEnvelope bounds = null;
	protected ArrayList<SimpleFeature> simpleFeatures;
	protected SimpleFeatureType simpleFeatureType = null;
	
	public SimplifiedSimpleFeatureCollection() {
		this.simpleFeatures = new ArrayList<>();
	}
	
	public void addSimpleFeature(SimpleFeature feature) {
		this.simpleFeatures.add(feature);
	}
	
	public void setSchema(SimpleFeatureType schema) {
		this.simpleFeatureType = schema;
	}
	
	@Override
	public SimpleFeatureType getSchema() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void setID(String id) {
		this.id = id;
	}

	@Override
	public String getID() {
		return this.id;
	}

	@Override
	public void accepts(FeatureVisitor visitor, ProgressListener progress) throws IOException {
		throw new NotImplementedException("This accepts function in SimplifiedSimpleFeatureCollection in not realized!!");
	}
	
	public void setBounds(ReferencedEnvelope bounds) {
		this.bounds = bounds;
	}
	
	@Override
	public ReferencedEnvelope getBounds() {
		return this.bounds;
	}

	@Override
	public boolean contains(Object o) {
		return this.simpleFeatures.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> o) {
		return this.simpleFeatures.containsAll(o);
	}

	@Override
	public boolean isEmpty() {
		return this.simpleFeatures.size()==0;
	}

	@Override
	public int size() {
		return this.simpleFeatures.size();
	}

	@Override
	public Object[] toArray() {
		return this.simpleFeatures.toArray();
	}

	@Override
	public <O> O[] toArray(O[] a) {
		return this.simpleFeatures.toArray(a);
	}

	@Override
	public SimpleFeatureIterator features() {
		SimpleFeatureIteratorImpl impl = new SimpleFeatureIteratorImpl(simpleFeatures);
		return impl;
	}

	@Override
	public SimpleFeatureCollection subCollection(Filter filter) {
		throw new NotImplementedException("This subCollection function in SimplifiedSimpleFeatureCollection in not realized!!");
	}

	@Override
	public SimpleFeatureCollection sort(SortBy order) {
		throw new NotImplementedException("This sort function in SimplifiedSimpleFeatureCollection in not realized!!");
	}

}
