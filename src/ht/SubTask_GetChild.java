package ht;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public class SubTask_GetChild
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

  SubTask_GetChild(int taskID, CountDownLatch latch, ArrayList<RegionEncoding> rc, ArrayList<IndexNode>[] MNode, NameIDtable nameIDtable, String nameString, ArrayList<Integer> input_ids, ArrayList<Integer> result_ids)
  {
    this.taskID = taskID;
    this.latch = latch;
    this.rc = rc;
    this.MNode = MNode;
    this.nameIDtable = nameIDtable;
    this.input_ids = input_ids;
    this.nameString = nameString;
    this.result_ids = result_ids;
  }

  public void run()
  {
    Thread a = Thread.currentThread();
    System.out.println("thread =" + a);
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
    int input_len = this.input_ids.size();

    for (int i = 0; i < input_len; i++) {
      int nodeMID = ((Integer)this.input_ids.get(i)).intValue();

      if ((this.isGetFollow) && (((RegionEncoding)this.rc.get(nodeMID)).nameID == nameID)) {
        this.result_ids.add(Integer.valueOf(nodeMID));
      }
      ArrayList tmpList = this.MNode[nodeMID];
      int len = tmpList.size();

      for (int j = 0; j < len; j++) {
        IndexNode tmp = (IndexNode)tmpList.get(j);
        int nodeNID = tmp.getNNodeID();
        relation = tmp.getRelationType();
        if ((nameTest) || ((((RegionEncoding)this.rc.get(nodeNID)).nameID == nameID) && (tmp.getRelationType() == CoPXQ.RTYPE_CHILD))) {
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
    System.out.println("{{SubTask_GetChild}} Subtask" + this.taskID + " run time:=(ms)" + (ty - tx));
  }
}