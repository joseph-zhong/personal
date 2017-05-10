package edu.washington.cs.grail.relative_size.dataset;

public class ObjectPair {
	private String biggerObject;
	private String smallerObject;
	
	public ObjectPair(String biggerObject, String smallerObject) {
		this.biggerObject = biggerObject;
		this.smallerObject = smallerObject;
	}
	
	public String getBiggerObject() {
		return biggerObject;
	}
	
	public String getSmallerObject() {
		return smallerObject;
	}
	
	@Override
	public String toString() {
		return getBiggerObject() + " > " + getSmallerObject();
	}
}
