package ht;
/**
*  Entrance of experiments.
*   coPXQ (cost-based Parallel XPath Query)
*   htprj202006
*   date: 2020.06
*   author: Rongxin Chen
 * 
 */

public class MainTest {

	public static void main(String[] args) {
		int TASK_NUMBER=4;

		CoPXQ test = new CoPXQ("d:/xmldata/xmark86m.xml");

		int blockStartPos = 38;
		int blockEndPos = test.fileLen;
		test.ContinueReadingTag(blockStartPos, blockEndPos);
		test.CreateIndexParallel_new(TASK_NUMBER);
		test.Test_X4();

		test.finishAll();
	}

}
