package ht;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Vector;

public class NameIDtable
{
  public HashMap<Key, Key> nameMap = new HashMap();
  Vector<String> names = new Vector(32, 16);
  Key probe = new Key(null);

  public int enterName(String nameString) {
    this.probe.nameString = nameString;
    Key key = (Key)this.nameMap.get(this.probe);
    if (key != null)
      return key.code;
    return addName(nameString);
  }

  private int addName(String nameString) {
    Key key = new Key(nameString);
    key.code = this.names.size();
    this.names.addElement(nameString);
    this.nameMap.put(key, key);
    return key.code;
  }

  public int find(String nameString) {
    this.probe.nameString = nameString;
    Key key = (Key)this.nameMap.get(this.probe);
    return key != null ? key.code : -1;
  }

  public String getName(int rank) {
    if ((rank < 0) || (rank >= this.names.size()))
      throw new IllegalArgumentException("bad rank " + rank);
    return (String)this.names.elementAt(rank);
  }

  public static void main(String[] args)
  {
    NameIDtable nt = new NameIDtable();

    String[] names = { "site", "loc", "loc", "loc", "name", "num" };
    int len = names.length;
    for (int i = 0; i < len; i++) {
      nt.enterName(names[i]);
    }
    System.out.println("get res= " + nt.find("loc"));
    System.out.println("get res= " + nt.getName(2));
  }
}

class Key
{
  String nameString;
  int code;

  Key(String nameString)
  {
    this.nameString = nameString;
  }
  public int hashCode() {
    return this.nameString.hashCode();
  }
  public boolean equals(Object other) {
    if ((other == null) || (!(other instanceof Key)))
      return false;
    Key n = (Key)other;
    return this.nameString.equals(n.nameString);
  }
}