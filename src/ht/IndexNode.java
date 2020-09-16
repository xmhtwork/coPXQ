package ht;

import java.io.Serializable;

public class IndexNode   implements Serializable{

	private static final long serialVersionUID = 5563758624458335375L;
	private byte relationType;
	private int NNodeID;  // NameTest NodeID
	
	public IndexNode(){
		this.relationType=-1;
		this.NNodeID=-1;
	}
	public IndexNode(byte relationType ,int NNodeID){
		this.relationType=relationType;
		this.NNodeID=NNodeID;
	}
	
	public byte  getRelationType() {
		return relationType;
	}
	public void setRelationType(byte relationType) {
		this.relationType = relationType;
	}
	public int getNNodeID() {
		return NNodeID;
	}
	public void setNNodeID(int nodeID) {
		NNodeID = nodeID;
	}
}
