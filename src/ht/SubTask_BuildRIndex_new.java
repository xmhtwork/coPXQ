package ht;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public class SubTask_BuildRIndex_new
  implements Runnable
{
  int taskID;
  long allCalc;
  long count;
  CountDownLatch latch;
  ArrayList<RegionEncoding> rc;
  ArrayList<IndexNode>[] MNode;
  int len;
  Position start;
  Position end;

  SubTask_BuildRIndex_new(int taskID, CountDownLatch latch, ArrayList<RegionEncoding> rc, ArrayList<IndexNode>[] MNode, Position start, Position end, int len)
  {
    this.taskID = taskID;
    this.latch = latch;
    this.rc = rc;
    this.MNode = MNode;
    this.len = len;
    this.start = start;
    this.end = end;
  }

  byte CalcRelation(RegionEncoding nodeM, RegionEncoding nodeN)
  {
    byte relation = 0;

    if ((nodeM.startPos < nodeN.startPos) && (nodeN.startPos < nodeM.endPos))
    {
      if ((nodeM.level != nodeN.level - 1) && (!nodeN.isAttribute))
        relation = 1;
      else if ((nodeM.level == nodeN.level - 1) && (!nodeN.isAttribute))
        relation = 2;
      else if ((nodeM.level == nodeN.level - 1) && (nodeN.isAttribute))
        relation = 3;
      else if ((nodeM.level != nodeN.level - 1) && (nodeN.isAttribute))
        relation = 9;
      else relation = 0;
    }
    return relation;
  }

  public void run()
  {
    Thread a = Thread.currentThread();
    System.out.println("thread =" + a);
    long tx = System.currentTimeMillis();

    byte relation = 0;

    if (this.start.i != this.end.i) {
      RegionEncoding rcM = (RegionEncoding)this.rc.get(this.start.i);
      for (int j = this.start.j + 1; j < this.len; j++) {
        RegionEncoding rcN = (RegionEncoding)this.rc.get(j);
        relation = CalcRelation(rcM, rcN);
        this.allCalc += 1L;
        if ((relation != 0) && (relation != 9)) {
          IndexNode tmpM = new IndexNode(relation, rcN.id);
          this.MNode[this.start.i].add(tmpM);
          this.count += 1L;
        }
        if (relation == 0) {
          break;
        }
      }
      for (int i = this.start.i + 1; i < this.end.i - 1; i++) {
        rcM = (RegionEncoding)this.rc.get(i);
        for (int j = i + 1; j < this.len; j++) {
          RegionEncoding rcN = (RegionEncoding)this.rc.get(j);
          relation = CalcRelation(rcM, rcN);
          this.allCalc += 1L;
          if ((relation != 0) && (relation != 9)) {
            IndexNode tmpM = new IndexNode(relation, rcN.id);
            this.MNode[i].add(tmpM);
            this.count += 1L;
          }
          if (relation == 0)
            break;
        }
      }
      rcM = (RegionEncoding)this.rc.get(this.end.i);
      for (int j = this.end.i + 1; j < this.end.j; j++) {
        RegionEncoding rcN = (RegionEncoding)this.rc.get(j);
        relation = CalcRelation(rcM, rcN);
        this.allCalc += 1L;
        if ((relation != 0) && (relation != 9)) {
          IndexNode tmpM = new IndexNode(relation, rcN.id);
          this.MNode[this.end.i].add(tmpM);
          this.count += 1L;
        }
        if (relation == 0) break;
      }
    }
    else {
      RegionEncoding rcM = (RegionEncoding)this.rc.get(this.start.i);
      for (int j = this.start.j + 1; j < this.end.j; j++) {
        RegionEncoding rcN = (RegionEncoding)this.rc.get(j);
        relation = CalcRelation(rcM, rcN);
        this.allCalc += 1L;
        if ((relation != 0) && (relation != 9)) {
          IndexNode tmpM = new IndexNode(relation, rcN.id);
          this.MNode[this.start.i].add(tmpM);
          this.count += 1L;
        }
        if (relation == 0)
        {
          break;
        }
      }

    }

    this.latch.countDown();
    long ty = System.currentTimeMillis();
    System.out.println("{{SubTask_CreateIndex}} Subtask" + this.taskID + " run time:=(ms)" + (ty - tx));
  }
}