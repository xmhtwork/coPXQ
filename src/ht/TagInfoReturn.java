package ht;

public class TagInfoReturn {
	private int pos; 
	private String str;
	
	public TagInfoReturn(int pos, String str) {
		super();
		this.pos = pos;
		this.str = str;
	}

	public int getPos() {
		return pos;
	}

	public void setPos(int pos) {
		this.pos = pos;
	}

	public String getStr() {
		return str;
	}

	public void setStr(String str) {
		this.str = str;
	}
}
