package ht;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public class SubTask_GetDescendant_new
  implements Runnable
{
  int taskID;
  CountDownLatch latch;
  ArrayList<RegionEncoding> rc;
  ArrayList<IndexNode>[] MNode;
  NameIDtable nameIDtable;
  ArrayList<Integer> input_ids;
  String nameString;
  boolean isGetFollow = true;
  ArrayList<Integer> result_ids;
  Position start;
  Position end;

  SubTask_GetDescendant_new(int taskID, CountDownLatch latch, ArrayList<RegionEncoding> rc, ArrayList<IndexNode>[] MNode, NameIDtable nameIDtable, String nameString, ArrayList<Integer> input_ids, ArrayList<Integer> result_ids, Position start, Position end)
  {
    this.taskID = taskID;
    this.latch = latch;
    this.rc = rc;
    this.MNode = MNode;
    this.nameIDtable = nameIDtable;
    this.input_ids = input_ids;
    this.nameString = nameString;
    this.result_ids = result_ids;

    this.start = start;
    this.end = end;
  }

  public void run()
  {
    Thread a = Thread.currentThread();
    System.out.println("[htht]_thread =" + a);
    long tx = System.currentTimeMillis();

    boolean nameTest = false;
    int nameID;
    if (this.nameString.compareTo(new String("*")) == 0)
    {
      nameTest = true;
      nameID = -1;
    } else {
      nameID = this.nameIDtable.enterName(this.nameString);
    }

    int relation = 0;

    if (this.start.i != this.end.i)
    {
      int nodeMID = ((Integer)this.input_ids.get(this.start.i)).intValue();
      if ((this.isGetFollow) && (((RegionEncoding)this.rc.get(nodeMID)).nameID == nameID))
        this.result_ids.add(Integer.valueOf(nodeMID));
      ArrayList tmpList = this.MNode[nodeMID];
      int len = tmpList.size();

      for (int j = this.start.j; j < len; j++) {
        IndexNode tmp = (IndexNode)tmpList.get(j);
        int nodeNID = tmp.getNNodeID();
        relation = tmp.getRelationType();
        if ((nameTest) || ((((RegionEncoding)this.rc.get(nodeNID)).nameID == nameID) && ((relation == CoPXQ.RTYPE_DESCENDANT) || (relation == CoPXQ.RTYPE_CHILD)))) {
          if (this.isGetFollow) {
            this.result_ids.add(new Integer(nodeNID));
          } else {
            this.result_ids.add(new Integer(nodeMID));
            break;
          }

        }

      }

      for (int i = this.start.i + 1; i < this.end.i - 1; i++) {
        nodeMID = ((Integer)this.input_ids.get(i)).intValue();

        if ((this.isGetFollow) && (((RegionEncoding)this.rc.get(nodeMID)).nameID == nameID))
          this.result_ids.add(Integer.valueOf(nodeMID));
        tmpList = this.MNode[nodeMID];
        len = tmpList.size();

        for (int j = 0; j < len; j++) {
          IndexNode tmp = (IndexNode)tmpList.get(j);
          int nodeNID = tmp.getNNodeID();
          relation = tmp.getRelationType();
          if ((nameTest) || ((((RegionEncoding)this.rc.get(nodeNID)).nameID == nameID) && ((relation == CoPXQ.RTYPE_DESCENDANT) || (relation == CoPXQ.RTYPE_CHILD)))) {
            if (this.isGetFollow) {
              this.result_ids.add(new Integer(nodeNID));
            } else {
              this.result_ids.add(new Integer(nodeMID));
              break;
            }

          }

        }

      }

      nodeMID = ((Integer)this.input_ids.get(this.end.i)).intValue();
      if ((this.isGetFollow) && (((RegionEncoding)this.rc.get(nodeMID)).nameID == nameID))
        this.result_ids.add(Integer.valueOf(nodeMID));
      tmpList = this.MNode[nodeMID];
      len = tmpList.size();

      for (int j = 0; j < this.end.j; j++) {
        IndexNode tmp = (IndexNode)tmpList.get(j);
        int nodeNID = tmp.getNNodeID();
        relation = tmp.getRelationType();
        if ((nameTest) || ((((RegionEncoding)this.rc.get(nodeNID)).nameID == nameID) && ((relation == CoPXQ.RTYPE_DESCENDANT) || (relation == CoPXQ.RTYPE_CHILD)))) {
          if (this.isGetFollow) {
            this.result_ids.add(new Integer(nodeNID));
          } else {
            this.result_ids.add(new Integer(nodeMID));
            break;
          }
        }

      }

    }
    else
    {
      int nodeMID = ((Integer)this.input_ids.get(this.start.i)).intValue();
      if ((this.isGetFollow) && (((RegionEncoding)this.rc.get(nodeMID)).nameID == nameID))
        this.result_ids.add(Integer.valueOf(nodeMID));
      ArrayList tmpList = this.MNode[nodeMID];
      int len = tmpList.size();

      for (int j = this.start.j; j < this.end.j; j++) {
        IndexNode tmp = (IndexNode)tmpList.get(j);
        int nodeNID = tmp.getNNodeID();
        relation = tmp.getRelationType();
        if ((nameTest) || ((((RegionEncoding)this.rc.get(nodeNID)).nameID == nameID) && ((relation == CoPXQ.RTYPE_DESCENDANT) || (relation == CoPXQ.RTYPE_CHILD)))) {
          if (this.isGetFollow) {
            this.result_ids.add(new Integer(nodeNID));
          } else {
            this.result_ids.add(new Integer(nodeMID));
            break;
          }

        }

      }

    }

    this.latch.countDown();
    long ty = System.currentTimeMillis();
    System.out.println("{{SubTask_GetDescendant}} Subtask" + this.taskID + " run time:=(ms)" + (ty - tx));
  }
}