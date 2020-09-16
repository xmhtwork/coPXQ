package ht;

public class TagInfo {
	private String tagName;
	private  int tagType;
	private int Pos;
	public String getTagName() {
		return tagName;
	}

	public void setTagName(String tagName) {
		this.tagName = tagName;
	}

	public int getTagType() {
		return tagType;
	}

	public void setTagType(int tagType) {
		this.tagType = tagType;
	}

	public int getPos() {
		return Pos;
	}

	public void setPos(int pos) {
		Pos = pos;
	}

	public int getTagID() {
		return tagID;
	}

	public void setTagID(int tagID) {
		this.tagID = tagID;
	}

	private int tagID;
	
	public TagInfo(){
		this.tagID=-1;
		this.tagName="NIL";
		this.Pos=-1;
		this.tagType=-1;
	}
}
