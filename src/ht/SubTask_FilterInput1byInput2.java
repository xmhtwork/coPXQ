package ht;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public class SubTask_FilterInput1byInput2
  implements Runnable
{
  int taskID;
  CountDownLatch latch;
  ArrayList<RegionEncoding> rc;
  ArrayList<IndexNode>[] MNode;
  ArrayList<Integer> input1_ids;
  ArrayList<Integer> input2_ids_sorted;
  ArrayList<Integer> result_ids;

  SubTask_FilterInput1byInput2(int taskID, CountDownLatch latch, ArrayList<RegionEncoding> rc, ArrayList<IndexNode>[] MNode, ArrayList<Integer> input1_ids, ArrayList<Integer> input2_ids_sorted, ArrayList<Integer> result_ids)
  {
    this.taskID = taskID;
    this.latch = latch;
    this.rc = rc;
    this.MNode = MNode;
    this.input1_ids = input1_ids;
    this.input2_ids_sorted = input2_ids_sorted;
    this.result_ids = result_ids;
  }

  public void run()
  {
    Thread a = Thread.currentThread();
    System.out.println("thread =" + a);
    long tx = System.currentTimeMillis();

    int len1 = this.input1_ids.size();

    int len2 = this.input2_ids_sorted.size();
    int[] input = new int[len2];

    for (int i = 0; i < len2; i++) {
      input[i] = ((Integer)this.input2_ids_sorted.get(i)).intValue();
    }

    for (int i = 0; i < len1; i++) {
      Integer tmpInput = (Integer)this.input1_ids.get(i);
      int tmpID = tmpInput.intValue();

      ArrayList tmpList = this.MNode[tmpID];
      int tmpLen = tmpList.size();
      for (int j = 0; j < tmpLen; j++) {
        IndexNode tmpMNode = (IndexNode)tmpList.get(j);
        int tmpNID = tmpMNode.getNNodeID();
        byte relation = tmpMNode.getRelationType();

        if (((relation != CoPXQ.RTYPE_CHILD) && (relation != CoPXQ.RTYPE_DESCENDANT)) || (!CoPXQ.isIntAinList(tmpNID, input)))
          continue;
        this.result_ids.add(tmpInput);
        break;
      }

    }

    this.latch.countDown();
    long ty = System.currentTimeMillis();
    System.out.println("{{SubTask_FilterInput1byInput2}} Subtask" + this.taskID + " run time:=(ms)" + (ty - tx));
  }
}