package ht;

import java.io.Serializable;

public class RegionEncoding
  implements Serializable
{
  private static final long serialVersionUID = -6275613475978488731L;
  int nameID;
  int id;
  int startPos;
  int endPos;
  int level;
  boolean isAttribute = false;
  int contained_number_DS_CH;
  int contained_number_AT;
  int contained_number_NAT;

  public RegionEncoding()
  {
    this.nameID = -1;
    this.id = -1;
    this.startPos = -1;
    this.endPos = -1;
    this.level = -1;
    this.isAttribute = false;
  }

  public int getContained_number_build() {
    return this.contained_number_DS_CH + this.contained_number_AT + this.contained_number_NAT;
  }

  public int getContained_number_save() {
    return this.contained_number_DS_CH + this.contained_number_AT;
  }
}