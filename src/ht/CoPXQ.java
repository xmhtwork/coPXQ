package ht;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Stack;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CoPXQ  program.
 * htprj202006
 *
 */
public class CoPXQ {
	static int TASK_NUMBER = 4;
	private static final int index = 0;
	static byte RTYPE_DESCENDANT = 1;
	static byte RTYPE_ANCESTOR = -1;
	static byte RTYPE_CHILD = 2;
	static byte RTYPE_PARENT = -2;
	static byte RTYPE_AATTRIB = 3;
	static byte RTYPE_RATTRIB = -3;
	static byte RTYPE_FSIBLING = 4;
	static byte RTYPE_PSIBLING = -4;

	boolean isFirstTagInBlock = true;
	String headString;
	String tailString;
	int lastPos = 0;
	MappedByteBuffer out;
	int fileLen;
	ArrayList<RegionEncoding> rc;
	NameIDtable nameIDtable;
	ArrayList<IndexNode>[] MNode;
	int totalRelationNum;
	public int cost_all_createIndex;
	private TagInfo tmpTag;
	private Stack<TagInfo> stack;
	private RegionEncoding tmpRC;
	private int level = 0;
	private int ID = 0;
	ExecutorService exec;

	public CoPXQ(String fileName) {
		this.out = GetMappedFile(fileName);
		this.rc = new ArrayList();
		this.nameIDtable = new NameIDtable();
		this.stack = new Stack();

		this.tmpRC = new RegionEncoding();
		this.tmpTag = new TagInfo();

		this.exec = Executors.newFixedThreadPool(TASK_NUMBER);
	}

	public void finishAll() {
		if (this.exec != null)
			this.exec.shutdown();
	}

	void printRCcode() {
		int len = this.rc.size();
		for (int i = 0; i < len; i++) {
			this.tmpRC = ((RegionEncoding) this.rc.get(i));

			if (this.tmpRC.level <= 1)
				System.out.println("ID=" + this.tmpRC.id + "; level=" + this.tmpRC.level + ";  contained_number_save="
						+ this.tmpRC.getContained_number_save() + ";   getContained_number_build="
						+ this.tmpRC.getContained_number_build());
		}
	}

	MappedByteBuffer GetMappedFile(String filename) {
		try {
			File fp = new File(filename);
			int fl = (int) fp.length();
			this.fileLen = fl;
			System.out.println("file size= " + fl);

			this.out = new RandomAccessFile(filename, "r").getChannel().map(FileChannel.MapMode.READ_ONLY, 0L, fl);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return this.out;
	}

	void ParseTagContent(int position, String tagStr) {
		int tagStrLen = tagStr.length();
		String str = tagStr.substring(1, tagStr.length() - 1).trim();

		String tagName = new String();
		int tagType = 0;

		int i = 0;
		char tc = '\000';
		StringBuffer tmpStr = new StringBuffer();
		String attribList = new String();

		if (str.charAt(0) == '/') {
			tagName = str.substring(1);
			tagType = 1;
		} else {
			i = 0;
			int len = str.length();
			boolean maybe3and5 = false;
			boolean maybe4and6 = false;
			if (str.charAt(len - 1) == '/')
				maybe3and5 = true;
			else {
				maybe4and6 = true;
			}
			i = 0;
			if (maybe3and5) {
				str = str.substring(0, len - 1).trim();
			}
			len = str.length();

			while ((tc != ' ') && (i < len)) {
				tc = str.charAt(i);
				tmpStr.append(tc);
				i++;
			}
			tagName = tmpStr.toString().trim();

			if (maybe3and5) {
				if (i == len) {
					tagType = 3;
				} else {
					tagType = 5;
					attribList = str.substring(i, len).trim();
				}
			} else if (maybe4and6) {
				if (i == len) {
					tagType = 2;
				} else {
					tagType = 4;
					attribList = str.substring(i, len).trim();
				}

			}

		}

		if (tagType == 1) {
			if (this.stack.empty()) {
				System.out.println("==>msmatched: tagName[" + tagName + "], position=" + position);
			} else {
				this.tmpTag = ((TagInfo) this.stack.peek());
				if (this.tmpTag.getTagName().equals(tagName)) {
					this.tmpTag = ((TagInfo) this.stack.pop());
					this.level -= 1;

					int ccc0_DS_CH = 0;
					int ccc0_AT = 0;
					int ccc0_NAT = 0;

					int currentNodeNum = this.rc.size();

					for (int m = currentNodeNum - 1; m > -1; m--) {
						RegionEncoding checkNode = (RegionEncoding) this.rc.get(m);

						if (!checkNode.isAttribute)
							ccc0_DS_CH++;
						if ((checkNode.isAttribute) && (checkNode.level - 1 == this.level))
							ccc0_AT++;
						if ((checkNode.isAttribute) && (checkNode.level - 1 != this.level)) {
							ccc0_NAT++;
						}

						if (checkNode.level == this.level) {
							checkNode.endPos = (position + tagStrLen);

							checkNode.contained_number_DS_CH = ccc0_DS_CH;
							checkNode.contained_number_AT = ccc0_AT;
							checkNode.contained_number_NAT = ccc0_NAT;

							this.cost_all_createIndex += ccc0_DS_CH + ccc0_AT + ccc0_NAT + 1;

							break;
						}

					}

				}

			}

		} else if ((tagType == 2) || (tagType == 4)) {
			TagInfo newTag = new TagInfo();

			newTag.setTagName(tagName);
			newTag.setTagType(tagType);
			newTag.setPos(position);

			int newNameID = this.nameIDtable.enterName(tagName);

			RegionEncoding newRC = new RegionEncoding();
			newRC.startPos = position;

			newRC.nameID = newNameID;
			newRC.level = this.level;
			newRC.id = this.ID;
			this.rc.add(newRC);
			this.ID += 1;

			this.stack.push(newTag);
			this.level += 1;

			if (tagType == 4) {
				ProcessAttribList(i + position, attribList);
			}
		} else if ((tagType == 3) || (tagType == 5)) {
			TagInfo newTag = new TagInfo();

			newTag.setTagName(tagName);
			newTag.setTagType(tagType);
			newTag.setPos(position);

			int newNameID = this.nameIDtable.enterName(tagName);

			RegionEncoding newRC = new RegionEncoding();
			newRC.startPos = position;
			newRC.endPos = (position + tagStrLen);

			newRC.nameID = newNameID;
			newRC.level = this.level;
			newRC.id = this.ID;

			this.rc.add(newRC);
			this.ID += 1;

			if (tagType == 5)
				ProcessAttribList(i + position, attribList);
		}
	}

	void ProcessAttribList(int startPos, String attribList) {
		int len = attribList.length();

		int tagType = 0;
		int posSpan = 0;

		attribList = attribList.replace('"', '\'');

		while (len != 0) {
			int pos = attribList.indexOf('=');
			String tmpName = attribList.substring(0, pos).trim();

			int pos0 = attribList.indexOf('\'');
			int pos1 = attribList.indexOf('\'', pos0 + 1);
			String tmpValue = attribList.substring(pos0 + 1, pos1).trim();

			int newNameID = this.nameIDtable.enterName(tmpName);

			RegionEncoding newRC = new RegionEncoding();
			newRC.startPos = (startPos + posSpan);
			newRC.endPos = (startPos + posSpan + pos1 + 2);

			newRC.nameID = newNameID;
			newRC.level = this.level;
			newRC.id = this.ID;
			newRC.isAttribute = true;

			this.rc.add(newRC);
			this.ID += 1;

			attribList = attribList.substring(pos1 + 1, len).trim();
			int lenNew = attribList.length();
			posSpan = posSpan + len - lenNew;
			len = lenNew;
		}
	}

	String ReadStringAB(int from, int to) {
		int i = from;
		StringBuffer str = new StringBuffer();
		while (i < to) {
			str.append((char) this.out.get(i));
			i++;
		}
		return str.toString();
	}

	int ReadUntilFirstCharA(int startPos, char A, int blockEndPos) {
		char tc = '\000';
		int i = startPos;
		int endPos = -1;
		while (i < blockEndPos) {
			tc = (char) this.out.get(i);
			if (tc == A) {
				endPos = i + 1;
				break;
			}
			i++;
		}
		return endPos;
	}

	TagInfoReturn ReadFirstTag(int startPos, int blockEndPos) {
		char tc = '\000';
		int i = startPos;
		int tmpPos0 = 0;
		int tmpPos1 = 0;
		int tmpPos2 = 0;
		int pos = 0;

		String tagStr = "NIL_NIL";

		StringBuffer headString = new StringBuffer();

		while (i < blockEndPos) {
			tc = (char) this.out.get(i);
			if ((this.isFirstTagInBlock) && (tc != '<')) {
				headString.append(tc);
			}
			if (tc == '<') {
				startPos = i;
				pos = ReadUntilFirstCharA(i, '>', blockEndPos);
				break;
			}
			i++;
		}
		if (pos == -1) {
			System.err.println("tag been trimed!");
			pos = blockEndPos;
		} else if (i == blockEndPos) {
			System.err.println("content  been trimed!");
			pos = blockEndPos;
		} else {
			tagStr = ReadStringAB(startPos, pos);

			if (this.isFirstTagInBlock) {
				this.headString = headString.toString();
				System.err.println("head nametag: " + headString);
				this.isFirstTagInBlock = false;
			}
			this.lastPos = pos;

			ParseTagContent(startPos, tagStr);
		}

		TagInfoReturn rt = new TagInfoReturn(pos, tagStr);
		return rt;
	}

	void ContinueReadingTag(int startPos, int blockEndPos) {
		int i = startPos;

		while (i != blockEndPos) {
			TagInfoReturn rt = ReadFirstTag(i, blockEndPos);
			i = rt.getPos();
		}
		this.tailString = ReadStringAB(this.lastPos, blockEndPos);

		System.err.println("tail nametag:" + this.tailString);
	}

	byte CalcRelation(RegionEncoding nodeM, RegionEncoding nodeN) {
		byte relation = 0;
		if ((nodeM.startPos < nodeN.startPos) && (nodeN.startPos < nodeM.endPos)) {
			if ((nodeM.level != nodeN.level - 1) && (!nodeN.isAttribute))
				relation = 1;
			else if ((nodeM.level == nodeN.level - 1) && (!nodeN.isAttribute))
				relation = 2;
			else if ((nodeM.level == nodeN.level - 1) && (nodeN.isAttribute))
				relation = 3;
			else if ((nodeM.level != nodeN.level - 1) && (nodeN.isAttribute))
				relation = 9;
			else {
				relation = 0;
			}

		}

		return relation;
	}

	void CreateIndexParallel_new(int taskNum) {
		System.err.println("**********CreateIndexParallel_new: ");

		long xx = System.currentTimeMillis();
		int len = this.rc.size();
		this.MNode = ((ArrayList[]) Array.newInstance(ArrayList.class, len));
		for (int i = 0; i < len; i++) {
			this.MNode[i] = new ArrayList();
		}

		int more = taskNum;
		int Rb = 0;
		int Cm = (this.cost_all_createIndex + more) / taskNum;

		CountDownLatch latch = new CountDownLatch(taskNum);
		SubTask_BuildRIndex_new[] subTaskGroup = new SubTask_BuildRIndex_new[taskNum];

		Position[] P = new Position[taskNum - 1];
		int k = 0;
		int Rc = 0;

		for (RegionEncoding n : this.rc) {
			Rb = n.getContained_number_build();
			if (Rb != 0) {
				Rc += Rb + 1;
			}
			while (Rc >= Cm) {
				P[k] = new Position();
				P[k].i = n.id;
				P[k].j = (n.id + Rb - (Rc - Cm) - 1);

				if (k == 0) {
					Position start = new Position(0, 0);
					subTaskGroup[k] = new SubTask_BuildRIndex_new(k, latch, this.rc, this.MNode, start, P[0], len);
				} else if (k != taskNum - 1) {
					subTaskGroup[k] = new SubTask_BuildRIndex_new(k, latch, this.rc, this.MNode, P[(k - 1)], P[k], len);
				}
				this.exec.execute(subTaskGroup[k]);

				k++;
				Rc -= Cm;
				System.err.println("DONE");
			}
		}

		if (k == taskNum - 1) {
			int lastID = this.rc.size() - 1;
			Position end = new Position(lastID, ((RegionEncoding) this.rc.get(lastID)).getContained_number_build() - 1);
			subTaskGroup[k] = new SubTask_BuildRIndex_new(k, latch, this.rc, this.MNode, P[(k - 1)], end, len);
			this.exec.execute(subTaskGroup[k]);
		}
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		long yy = System.currentTimeMillis();
		System.out.println("\n==========>\t\t\t\t CreateIndexParallel_new--> used time(ms): " + (yy - xx));

	}

	void CreateIndexParallel(int taskNum) {
		System.err.println("**********CreateIndexParallel OLD");

		long xx = System.currentTimeMillis();

		byte relation = 0;
		long count = 0L;
		int len = this.rc.size();

		this.MNode = ((ArrayList[]) Array.newInstance(ArrayList.class, len));

		for (int i = 0; i < len; i++) {
			this.MNode[i] = new ArrayList();
		}
		CountDownLatch latch = new CountDownLatch(taskNum);

		int taskSize = len / taskNum;

		SubTask_BuildRIndex[] subTaskGroup = new SubTask_BuildRIndex[taskNum];
		for (int i = 0; i < taskNum; i++) {
			if (i != taskNum - 1)
				subTaskGroup[i] = new SubTask_BuildRIndex(i, latch, this.rc, this.MNode, i * taskSize,
						(i + 1) * taskSize - 1, len);
			else {
				subTaskGroup[i] = new SubTask_BuildRIndex(i, latch, this.rc, this.MNode, i * taskSize, len - 1, len);
			}

		}

		for (int k = 0; k < taskNum; k++)
			this.exec.execute(subTaskGroup[k]);
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		long yy = System.currentTimeMillis();
		System.out.println("\n==========>\t\t\t  CreateIndexParallel-->used time(ms): " + (yy-xx));
	
	}

	private int GetRelationNumFromTo_SAVE_byID(int from, int to) {
		int num = 0;
		for (int i = from; i <= to; i++) {
			num += ((RegionEncoding) this.rc.get(i)).getContained_number_save();
		}
		return num;
	}

	private int GetRelationNumFromTo_BUILD_byID(int from, int to) {
		int num = 0;
		for (int i = from; i <= to; i++) {
			num += ((RegionEncoding) this.rc.get(i)).getContained_number_build();
		}
		return num;
	}

	ArrayList<Integer> GetChild(ArrayList<Integer> input_ids, String nameString, boolean isGetFollow) {
		ArrayList result = new ArrayList();

		boolean nameTest = false;
		int nameID;

		if (nameString.compareTo(new String("*")) == 0) {
			nameTest = true;
			nameID = -1;
		} else {
			nameID = this.nameIDtable.enterName(nameString);
		}

		int input_len = input_ids.size();

		for (int i = 0; i < input_len; i++) {
			int nodeMID = ((Integer) input_ids.get(i)).intValue();

			ArrayList tmpList = this.MNode[nodeMID];
			int len = tmpList.size();

			for (int j = 0; j < len; j++) {
				IndexNode tmp = (IndexNode) tmpList.get(j);
				int nodeNID = tmp.getNNodeID();
				if ((nameTest) || ((((RegionEncoding) this.rc.get(nodeNID)).nameID == nameID)
						&& (tmp.getRelationType() == RTYPE_CHILD))) {
					if (isGetFollow) {
						result.add(new Integer(nodeNID));
					} else {
						result.add(new Integer(nodeMID));
						break;
					}
				}

			}

		}

		return result;
	}

	ArrayList<Integer> GetChild_parallel(ArrayList<Integer> input_ids, String nameString, boolean isGetFollow,
			int taskNum) {
		ArrayList[] resultPart = new ArrayList[taskNum];
		for (int i = 0; i < taskNum; i++) {
			resultPart[i] = new ArrayList();
		}
		int len = input_ids.size();
		int lenPerTask = len / taskNum;

		ArrayList[] inputPart = new ArrayList[taskNum];
		for (int i = 0; i < taskNum; i++) {
			inputPart[i] = new ArrayList();
		}

		for (int i = 0; i < len; i++) {
			int t = i / lenPerTask;
			t = t > taskNum - 1 ? taskNum - 1 : t;
			inputPart[t].add((Integer) input_ids.get(i));
		}

		CountDownLatch latch = new CountDownLatch(taskNum);

		SubTask_GetChild[] subTaskGroup = new SubTask_GetChild[taskNum];
		for (int i = 0; i < taskNum; i++) {
			subTaskGroup[i] = new SubTask_GetChild(i, latch, this.rc, this.MNode, this.nameIDtable, nameString,
					inputPart[i], resultPart[i]);
		}

		for (int k = 0; k < taskNum; k++)
			this.exec.execute(subTaskGroup[k]);
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		ArrayList result = new ArrayList();
		for (int i = 0; i < taskNum; i++) {
			result.addAll(resultPart[i]);
		}
		return result;
	}

	ArrayList<Integer> GetChild_attribute(ArrayList<Integer> input_ids, String nameString, boolean isGetFollow) {
		ArrayList result = new ArrayList();

		boolean nameTest = false;
		int nameID;
		if (nameString.compareTo(new String("*")) == 0) {
			nameTest = true;
			nameID = -1;
		} else {
			nameID = this.nameIDtable.enterName(nameString);
		}

		int input_len = input_ids.size();

		for (int i = 0; i < input_len; i++) {
			int nodeMID = ((Integer) input_ids.get(i)).intValue();

			ArrayList tmpList = this.MNode[nodeMID];
			int len = tmpList.size();

			for (int j = 0; j < len; j++) {
				IndexNode tmp = (IndexNode) tmpList.get(j);
				int nodeNID = tmp.getNNodeID();
				if ((nameTest) || ((((RegionEncoding) this.rc.get(nodeNID)).nameID == nameID)
						&& (tmp.getRelationType() == RTYPE_AATTRIB))) {
					if (isGetFollow) {
						result.add(new Integer(nodeNID));
					} else {
						result.add(new Integer(nodeMID));
						break;
					}
				}

			}

		}

		return result;
	}

	ArrayList<Integer> GetDescendant(ArrayList<Integer> input_ids, String nameString, boolean isGetFollow) {
		ArrayList result = new ArrayList();

		boolean nameTest = false;
		int nameID;
		if (nameString.compareTo(new String("*")) == 0) {
			nameTest = true;
			nameID = -1;
		} else {
			nameID = this.nameIDtable.enterName(nameString);
		}

		int relation = 0;
		int input_len = input_ids.size();

		for (int i = 0; i < input_len; i++) {
			int nodeMID = ((Integer) input_ids.get(i)).intValue();

			if ((isGetFollow) && (((RegionEncoding) this.rc.get(nodeMID)).nameID == nameID)) {
				result.add(Integer.valueOf(nodeMID));
			}
			ArrayList tmpList = this.MNode[nodeMID];
			int len = tmpList.size();

			for (int j = 0; j < len; j++) {
				IndexNode tmp = (IndexNode) tmpList.get(j);
				int nodeNID = tmp.getNNodeID();
				relation = tmp.getRelationType();
				if ((nameTest) || ((((RegionEncoding) this.rc.get(nodeNID)).nameID == nameID)
						&& ((relation == RTYPE_DESCENDANT) || (relation == RTYPE_CHILD)))) {
					if (isGetFollow) {
						result.add(new Integer(nodeNID));
					} else {
						result.add(new Integer(nodeMID));
						break;
					}
				}

			}

		}

		return result;
	}

	ArrayList<Integer> GetDescendant_parallel(ArrayList<Integer> input_ids, String nameString, boolean isGetFollow,
			int taskNum) {
		ArrayList[] resultPart = new ArrayList[taskNum];
		for (int i = 0; i < taskNum; i++) {
			resultPart[i] = new ArrayList();
		}
		int len = input_ids.size();
		int lenPerTask = len / taskNum;

		ArrayList[] inputPart = new ArrayList[taskNum];
		for (int i = 0; i < taskNum; i++) {
			inputPart[i] = new ArrayList();
		}

		for (int i = 0; i < len; i++) {
			int t = i / lenPerTask;
			t = t > taskNum - 1 ? taskNum - 1 : t;
			inputPart[t].add((Integer) input_ids.get(i));
		}

		CountDownLatch latch = new CountDownLatch(taskNum);

		SubTask_GetDescendant[] subTaskGroup = new SubTask_GetDescendant[taskNum];
		for (int i = 0; i < taskNum; i++) {
			subTaskGroup[i] = new SubTask_GetDescendant(i, latch, this.rc, this.MNode, this.nameIDtable, nameString,
					inputPart[i], resultPart[i]);
		}

		for (int k = 0; k < taskNum; k++)
			this.exec.execute(subTaskGroup[k]);
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		ArrayList result = new ArrayList();
		for (int i = 0; i < taskNum; i++) {
			result.addAll(resultPart[i]);
		}
		return result;
	}

	static int CalcThreadsNeeded(int threadAvaibleNum, double cost_inital, double cost_barrier, double cost_serial) {
		int threadsNum = 1;

		double minGain = 1.0D;
		for (int t = 1; t <= threadAvaibleNum; t++) {
			double gain = 1 / t + (cost_inital + t * cost_barrier) / cost_serial;
			if (gain >= 1.0D)
				break;
			if (gain < minGain) {
				minGain = gain;
				threadsNum = t;
			}
		}
		return threadsNum;
	}

	ArrayList<Integer> GetDescendant_parallel_new(ArrayList<Integer> input_ids, String nameString, boolean isGetFollow,
			int taskNum) {
		ArrayList[] resultPart = new ArrayList[taskNum];
		for (int i = 0; i < taskNum; i++) {
			resultPart[i] = new ArrayList();
		}
		CountDownLatch latch = new CountDownLatch(taskNum);
		SubTask_GetDescendant_new[] subTaskGroup = new SubTask_GetDescendant_new[taskNum];

		Collections.sort(input_ids);

		int more = taskNum;
		int cost_all = 0;

		for (Integer id : input_ids)
			cost_all += ((RegionEncoding) this.rc.get(id.intValue())).getContained_number_save();
		int Ct = (cost_all + more) / taskNum;

		Position[] P = new Position[taskNum - 1];
		int k = 0;
		int Rc = 0;
		int Rs = 0;
		int Pu = 0;
		for (Integer id : input_ids) {
			Rs = ((RegionEncoding) this.rc.get(id.intValue())).getContained_number_save();
			Rc += Rs;
			while (Rc >= Ct) {
				P[k] = new Position();
				P[k].i = Pu;
				P[k].j = (Rs - (Rc - Ct) - 1);

				if (k == 0) {
					Position start = new Position(0, 0);
					subTaskGroup[k] = new SubTask_GetDescendant_new(k, latch, this.rc, this.MNode, this.nameIDtable,
							nameString, input_ids, resultPart[k], start, P[0]);
				} else if (k != taskNum - 1) {
					subTaskGroup[k] = new SubTask_GetDescendant_new(k, latch, this.rc, this.MNode, this.nameIDtable,
							nameString, input_ids, resultPart[k], P[(k - 1)], P[k]);
				}
				this.exec.execute(subTaskGroup[k]);

				k++;
				Rc -= Ct;
				//System.err.println("DO>>>>>>>>>>new block");
			}
			Pu++;
		}
		if (k == taskNum - 1) {
			int lastPu = input_ids.size() - 1;
			int lastID = ((Integer) input_ids.get(lastPu)).intValue();
			int end_Iu = this.MNode[lastID].size() - 1;
			Position end = new Position(lastPu, end_Iu);
			subTaskGroup[k] = new SubTask_GetDescendant_new(k, latch, this.rc, this.MNode, this.nameIDtable, nameString,
					input_ids, resultPart[k], P[(k - 1)], end);
			this.exec.execute(subTaskGroup[k]);
		}

		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		ArrayList result = new ArrayList();
		for (int i = 0; i < taskNum; i++) {
			result.addAll(resultPart[i]);
		}
		return result;
	}

	ArrayList<Integer> FilterInput1byInput2(ArrayList<Integer> input1, ArrayList<Integer> input2) {
		ArrayList result = new ArrayList();

		int len1 = input1.size();

		Collections.sort(input2);
		int len2 = input2.size();
		int[] input = new int[len2];

		for (int i = 0; i < len2; i++) {
			input[i] = ((Integer) input2.get(i)).intValue();
		}

		for (int i = 0; i < len1; i++) {
			Integer tmpInput = (Integer) input1.get(i);
			int tmpID = tmpInput.intValue();

			ArrayList tmpList = this.MNode[tmpID];
			int tmpLen = tmpList.size();
			for (int j = 0; j < tmpLen; j++) {
				IndexNode tmpMNode = (IndexNode) tmpList.get(j);
				int tmpNID = tmpMNode.getNNodeID();
				byte relation = tmpMNode.getRelationType();

				if (((relation != RTYPE_CHILD) && (relation != RTYPE_DESCENDANT)) || (!isIntAinList(tmpNID, input)))
					continue;
				result.add(tmpInput);
				break;
			}

		}

		return result;
	}

	ArrayList<Integer> FilterInput1byInput2_AND(ArrayList<Integer> input1, ArrayList<Integer> input2,
			ArrayList<Integer> input3) {
		ArrayList result = new ArrayList();

		int len1 = input1.size();

		Collections.sort(input2);
		int len2 = input2.size();
		int[] input22 = new int[len2];
		for (int i = 0; i < len2; i++) {
			input22[i] = ((Integer) input2.get(i)).intValue();
		}

		Collections.sort(input3);
		int len3 = input3.size();
		int[] input33 = new int[len3];
		for (int i = 0; i < len3; i++) {
			input33[i] = ((Integer) input3.get(i)).intValue();
		}

		for (int i = 0; i < len1; i++) {
			Integer tmpInput = (Integer) input1.get(i);
			int tmpID = tmpInput.intValue();

			ArrayList tmpList = this.MNode[tmpID];
			int tmpLen = tmpList.size();
			for (int j = 0; j < tmpLen; j++) {
				IndexNode tmpMNode = (IndexNode) tmpList.get(j);
				int tmpNID = tmpMNode.getNNodeID();
				byte relation = tmpMNode.getRelationType();

				if (((relation != RTYPE_CHILD) && (relation != RTYPE_DESCENDANT)) || (!isIntAinList(tmpNID, input22))
						|| (!isIntAinList(tmpNID, input33)))
					continue;
				result.add(tmpInput);
				break;
			}
		}

		return result;
	}

	ArrayList<Integer> FilterInput1byInput2_OR(ArrayList<Integer> input1, ArrayList<Integer> input2,
			ArrayList<Integer> input3) {
		ArrayList result = new ArrayList();

		int len1 = input1.size();

		int len2 = input2.size();
		int[] input22 = new int[len2];
		for (int i = 0; i < len2; i++) {
			input22[i] = ((Integer) input2.get(i)).intValue();
		}

		int len3 = input3.size();
		int[] input33 = new int[len3];
		for (int i = 0; i < len3; i++) {
			input33[i] = ((Integer) input3.get(i)).intValue();
		}

		for (int i = 0; i < len1; i++) {
			Integer tmpInput = (Integer) input1.get(i);
			int tmpID = tmpInput.intValue();

			ArrayList tmpList = this.MNode[tmpID];
			int tmpLen = tmpList.size();
			for (int j = 0; j < tmpLen; j++) {
				IndexNode tmpMNode = (IndexNode) tmpList.get(j);
				int tmpNID = tmpMNode.getNNodeID();
				byte relation = tmpMNode.getRelationType();

				if (((relation != RTYPE_CHILD) && (relation != RTYPE_DESCENDANT))
						|| ((!isIntAinList(tmpNID, input22)) && (!isIntAinList(tmpNID, input33))))
					continue;
				result.add(tmpInput);
				break;
			}
		}

		return result;
	}

	ArrayList<Integer> FilterInput1byInput2_attribute(ArrayList<Integer> input1, ArrayList<Integer> input2) {
		ArrayList result = new ArrayList();

		int len1 = input1.size();

		Collections.sort(input2);
		int len2 = input2.size();
		int[] input = new int[len2];

		for (int i = 0; i < len2; i++) {
			input[i] = ((Integer) input2.get(i)).intValue();
		}

		for (int i = 0; i < len1; i++) {
			Integer tmpInput = (Integer) input1.get(i);
			int tmpID = tmpInput.intValue();

			ArrayList tmpList = this.MNode[tmpID];
			int tmpLen = tmpList.size();
			for (int j = 0; j < tmpLen; j++) {
				IndexNode tmpMNode = (IndexNode) tmpList.get(j);
				int tmpNID = tmpMNode.getNNodeID();
				byte relation = tmpMNode.getRelationType();

				if ((relation != RTYPE_AATTRIB) || (!isIntAinList(tmpNID, input)))
					continue;
				result.add(tmpInput);
				break;
			}

		}

		return result;
	}

	ArrayList<Integer> FilterInput1byInput2_parallel(ArrayList<Integer> input1, ArrayList<Integer> input2,
			int taskNum) {
		ArrayList[] resultPart = new ArrayList[taskNum];
		for (int i = 0; i < taskNum; i++) {
			resultPart[i] = new ArrayList();
		}
		int len = input1.size();
		int lenPerTask = len / taskNum;

		ArrayList[] inputPart = new ArrayList[taskNum];
		for (int i = 0; i < taskNum; i++) {
			inputPart[i] = new ArrayList();
		}
		for (int i = 0; i < len; i++) {
			int t = i / lenPerTask;
			t = t > taskNum - 1 ? taskNum - 1 : t;
			inputPart[t].add((Integer) input1.get(i));
		}

		Collections.sort(input2);

		CountDownLatch latch = new CountDownLatch(taskNum);

		SubTask_FilterInput1byInput2[] subTaskGroup = new SubTask_FilterInput1byInput2[taskNum];
		for (int i = 0; i < taskNum; i++) {
			subTaskGroup[i] = new SubTask_FilterInput1byInput2(i, latch, this.rc, this.MNode, inputPart[i], input2,
					resultPart[i]);
		}
		for (int k = 0; k < taskNum; k++)
			this.exec.execute(subTaskGroup[k]);
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		ArrayList result = new ArrayList();
		for (int i = 0; i < taskNum; i++) {
			result.addAll(resultPart[i]);
		}
		return result;
	}

	ArrayList<Integer> FilterInput1byInput2_checkPOS_biger(ArrayList<Integer> input1, ArrayList<Integer> input2) {
		ArrayList result = new ArrayList();

		int len1 = input1.size();

		Collections.sort(input2);
		int len2 = input2.size();

		for (int i = 0; i < len1; i++) {
			int tmpMID = ((Integer) input1.get(i)).intValue();

			for (int j = 0; j < len2; j++) {
				int tmpNID = ((Integer) input2.get(j)).intValue();

				if (tmpMID <= tmpNID)
					continue;
				result.add(Integer.valueOf(tmpMID));
				break;
			}

		}

		return result;
	}

	public static boolean isIntAinList(int A, int[] listA) {
		boolean isInside = true;

		int result = Arrays.binarySearch(listA, A);
		if (result < 0)
			isInside = false;
		return isInside;
	}

	ArrayList<Integer> GetAncestor(ArrayList<Integer> input_ids, String nameString, boolean isGetFollow) {
		ArrayList result = new ArrayList();
		int nodeMID = 0;

		boolean nameTest = false;
		int nameID;
		if (nameString.compareTo(new String("*")) == 0) {
			nameTest = true;
			nameID = -1;
		} else {
			nameID = this.nameIDtable.find(nameString);
		}

		int input_len = input_ids.size();
		ArrayList tmpList = new ArrayList();

		int maxInputID = ((Integer) Collections.max(input_ids)).intValue();

		for (int i = 0; i < maxInputID; i++) {
			if ((nameTest) || (((RegionEncoding) this.rc.get(i)).nameID == nameID))
				tmpList.add(new Integer(i));
		}
		result = FilterInput1byInput2(tmpList, input_ids);

		System.out.println("completed a --GetAncestor.");
		return result;
	}

	ArrayList<Integer> GetParent(ArrayList<Integer> input_ids, String nameString, boolean isGetFollow) {
		ArrayList result = new ArrayList();
		int nodeMID = 0;

		boolean nameTest = false;
		int nameID;
		if (nameString.compareTo(new String("*")) == 0) {
			nameTest = true;
			nameID = -1;
		} else {
			nameID = this.nameIDtable.find(nameString);
		}

		ArrayList tmpList = new ArrayList();

		int maxInputID = ((Integer) Collections.max(input_ids)).intValue();

		for (int i = 0; i < maxInputID; i++) {
			if ((nameTest) || (((RegionEncoding) this.rc.get(i)).nameID == nameID))
				tmpList.add(new Integer(i));
		}
		result = FilterInput1byInput2PC(tmpList, input_ids);

		return result;
	}

	ArrayList<Integer> FilterInput1byInput2PC(ArrayList<Integer> input1, ArrayList<Integer> input2) {
		ArrayList result = new ArrayList();

		int len1 = input1.size();

		Collections.sort(input2);
		int len2 = input2.size();
		int[] input = new int[len2];

		for (int i = 0; i < len2; i++) {
			input[i] = ((Integer) input2.get(i)).intValue();
		}

		for (int i = 0; i < len1; i++) {
			Integer tmpInput = (Integer) input1.get(i);
			int tmpID = tmpInput.intValue();

			ArrayList tmpList = this.MNode[tmpID];
			int tmpLen = tmpList.size();
			for (int j = 0; j < tmpLen; j++) {
				IndexNode tmpMNode = (IndexNode) tmpList.get(j);
				int tmpNID = tmpMNode.getNNodeID();
				byte relation = tmpMNode.getRelationType();

				if ((relation != RTYPE_CHILD) || (!isIntAinList(tmpNID, input)))
					continue;
				result.add(tmpInput);
				break;
			}
		}

		return result;
	}

	void Test_T1() {
		ArrayList input_doc = new ArrayList();
		input_doc.add(Integer.valueOf(0));
		long s001 = System.currentTimeMillis();
		ArrayList input1 = GetDescendant(input_doc, new String("S"), true);
		long s002 = System.currentTimeMillis();

		ArrayList input2 = GetDescendant_parallel(input1, new String("NP"), true, TASK_NUMBER);

		long s003 = System.currentTimeMillis();
		ArrayList input3 = GetChild(input2, new String("PP"), true);
		long s004 = System.currentTimeMillis();
		ArrayList input4 = GetChild(input3, new String("NN"), true);
		long s005 = System.currentTimeMillis();

		ArrayList last = input4;
		ArrayList result = new ArrayList();
		int len = last.size();

		if (len != 0) {
			Collections.sort(last);

			int tmp = ((Integer) last.get(0)).intValue();
			result.add(new Integer(tmp));
			for (int i = 1; i < len; i++) {
				int next = ((Integer) last.get(i)).intValue();
				if (tmp != next) {
					result.add(new Integer(next));
					tmp = next;
				}
			}
		}
		long s006 = System.currentTimeMillis();
		System.err.println("\n\n\n@@@T1 The result count= " + result.size());

		System.out.println(
				"\n############Test_T1 <<<<<<<<<<<<");
		System.out.print("input_doc=" + input_doc.size() + ";  R_num=" + GetRelationNum(input_doc));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s002 - s001));

		System.out.print("input1=" + input1.size() + ";  R_num=" + GetRelationNum(input1));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s003 - s002));

		System.out.print("input2=" + input2.size() + ";  R_num=" + GetRelationNum(input2));
		System.out.println("\t\t GetChild--->used time(ms): " + (s004 - s003));

		System.out.print("input3=" + input3.size() + ";  R_num=" + GetRelationNum(input3));
		System.out.println("\t\t GetChild--->used time(ms): " + (s005 - s004));

		System.out.println("\t\t Remove duplication--->used time(ms): " + (s006 - s005));

		System.out.println("\t\t @@@ Query total time(ms): " + (s006 - s001));
		System.out.println(">>>>>>>>>>>>>");
	}

	void Test_T2() {
		ArrayList input_doc = new ArrayList();
		input_doc.add(Integer.valueOf(0));
		long s001 = System.currentTimeMillis();
		ArrayList input1 = GetDescendant(input_doc, new String("S"), true);
		long s002 = System.currentTimeMillis();
		ArrayList input2 = GetChild(input1, new String("VBP"), false);
		long s003 = System.currentTimeMillis();
		ArrayList input3 = GetDescendant(input2, new String("NP"), false);
		long s004 = System.currentTimeMillis();
		ArrayList input4 = GetDescendant(input3, new String("VP"), true);
		long s005 = System.currentTimeMillis();
		ArrayList input5 = GetDescendant(input4, new String("PP"), true);
		long s006 = System.currentTimeMillis();
		ArrayList input6 = GetDescendant(input5, new String("IN"), false);
		long s007 = System.currentTimeMillis();
		ArrayList input7 = GetDescendant(input6, new String("NP"), true);
		long s008 = System.currentTimeMillis();
		ArrayList input8 = GetDescendant(input7, new String("VBN"), true);
		long s009 = System.currentTimeMillis();

		ArrayList last = input8;
		ArrayList result = new ArrayList();
		int len = last.size();

		if (len != 0) {
			Collections.sort(last);

			int tmp = ((Integer) last.get(0)).intValue();
			result.add(new Integer(tmp));
			for (int i = 1; i < len; i++) {
				int next = ((Integer) last.get(i)).intValue();
				if (tmp != next) {
					result.add(new Integer(next));
					tmp = next;
				}
			}
		}
		long s010 = System.currentTimeMillis();
		System.err.println("\n\n\n@@@ T2 The result count= " + result.size());

		System.out.println(
				"\n############Test_T2  <<<<<<<<<<<<<");
		System.out.print("input_doc=" + input_doc.size() + ";  R_num=" + GetRelationNum(input_doc));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s002 - s001));

		System.out.print("input1=" + input1.size() + ";  R_num=" + GetRelationNum(input1));
		System.out.println("\t\t GetChild--->used time(ms): " + (s003 - s002));

		System.out.print("input2=" + input2.size() + ";  R_num=" + GetRelationNum(input2));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s004 - s003));

		System.out.print("input3=" + input3.size() + ";  R_num=" + GetRelationNum(input3));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s005 - s004));

		System.out.print("input4=" + input4.size() + ";  R_num=" + GetRelationNum(input4));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s006 - s005));

		System.out.print("input5=" + input5.size() + ";  R_num=" + GetRelationNum(input5));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s007 - s006));

		System.out.print("input6=" + input6.size() + ";  R_num=" + GetRelationNum(input6));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s008 - s007));

		System.out.print("input7=" + input7.size() + ";  R_num=" + GetRelationNum(input7));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s009 - s008));

		System.out.println("\t\t Remove duplication--->used time(ms): " + (s010 - s009));

		System.out.println("\t\t @@@ Query total time(ms): " + (s010 - s001));
		System.out.println(">>>>>>>>>>>>>");
	}

	void Test_T2_filter() {
		ArrayList input_doc = new ArrayList();
		input_doc.add(Integer.valueOf(0));
		long s001 = System.currentTimeMillis();
		ArrayList input1 = GetDescendant(input_doc, new String("S"), true);
		long s002 = System.currentTimeMillis();
		ArrayList input2 = GetChild(input1, new String("VBP"), true);
		long s003 = System.currentTimeMillis();

		ArrayList predict1 = FilterInput1byInput2_parallel(input1, input2, TASK_NUMBER);
		long s004 = System.currentTimeMillis();
		ArrayList input3 = GetDescendant(predict1, new String("NP"), true);
		long s005 = System.currentTimeMillis();
		ArrayList predict2 = FilterInput1byInput2(predict1, input3);
		long s006 = System.currentTimeMillis();
		ArrayList input4 = GetDescendant(predict2, new String("VP"), true);
		long s007 = System.currentTimeMillis();
		ArrayList input5 = GetDescendant(input4, new String("PP"), true);
		long s008 = System.currentTimeMillis();
		ArrayList input6 = GetDescendant(input5, new String("IN"), true);
		long s009 = System.currentTimeMillis();
		ArrayList predict3 = FilterInput1byInput2(input5, input6);
		long s010 = System.currentTimeMillis();
		ArrayList input7 = GetDescendant(predict3, new String("NP"), true);
		long s011 = System.currentTimeMillis();
		ArrayList input8 = GetDescendant(input7, new String("VBN"), true);
		long s012 = System.currentTimeMillis();
		ArrayList last = input8;
		ArrayList result = new ArrayList();
		int len = last.size();

		if (len != 0) {
			Collections.sort(last);

			int tmp = ((Integer) last.get(0)).intValue();
			result.add(new Integer(tmp));
			for (int i = 1; i < len; i++) {
				int next = ((Integer) last.get(i)).intValue();
				if (tmp != next) {
					result.add(new Integer(next));
					tmp = next;
				}
			}
		}
		long s013 = System.currentTimeMillis();
		System.err.println("\n\n\n@@@ T2_filter The result count= " + result.size());

		System.out.println(
				"\n############Test_T2_filter<<<<<<<<<<<<<");
		System.out.println("input_doc=" + input_doc.size() + ";  R_num=" + GetRelationNum(input_doc));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s002 - s001));

		System.out.println("input1=" + input1.size() + ";  R_num=" + GetRelationNum(input1));
		System.out.println("\t\t GetChild--->used time(ms): " + (s003 - s002));

		System.out.print("input1=" + input1.size() + ";  R_num=" + GetRelationNum(input1) + "   &&   input2="
				+ input2.size() + ";  R_num=" + GetRelationNum(input2));
		System.out.println("\t\t FilterInput1byInput2--->used time(ms): " + (s004 - s003));

		System.out.println("predict1=" + predict1.size() + ";  R_num=" + GetRelationNum(predict1));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s005 - s004));

		System.out.print("predict1=" + predict1.size() + ";  R_num=" + GetRelationNum(predict1) + "   &&   input3="
				+ input3.size() + ";  R_num=" + GetRelationNum(input3));
		System.out.println("\t\t FilterInput1byInput2--->used time(ms): " + (s006 - s005));

		System.out.println("predict2=" + predict2.size() + ";  R_num=" + GetRelationNum(predict2));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s007 - s008));

		System.out.println("input4=" + input4.size() + ";  R_num=" + GetRelationNum(input4));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s008 - s007));

		System.out.println("input5=" + input5.size() + ";  R_num=" + GetRelationNum(input5));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s009 - s008));

		System.out.print("input5=" + input5.size() + ";  R_num=" + GetRelationNum(input5) + "   &&   input6="
				+ input6.size() + ";  R_num=" + GetRelationNum(input6));
		System.out.println("\t\t FilterInput1byInput2--->used time(ms): " + (s010 - s009));

		System.out.println("predict3=" + predict3.size() + ";  R_num=" + GetRelationNum(predict3));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s011 - s010));

		System.out.println("input7=" + input7.size() + ";  R_num=" + GetRelationNum(input7));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s012 - s011));

		System.out.println("\t\t Remove duplication--->used time(ms): " + (s013 - s012));

		System.out.println("\t\t @@@Query total time(ms): " + (s013 - s001));
		System.out.println(">>>>>>>>>>>>>");
	}

	void Test_T3() {
		ArrayList input_doc = new ArrayList();
		input_doc.add(Integer.valueOf(0));
		long s001 = System.currentTimeMillis();
		ArrayList input1 = GetDescendant(input_doc, new String("EMPTY"), true);

		long s002 = System.currentTimeMillis();
		ArrayList input2 = GetDescendant(input1, new String("VP"), true);

		long s003 = System.currentTimeMillis();
		ArrayList input3 = GetChild(input2, new String("PP"), true);
		long s004 = System.currentTimeMillis();
		ArrayList input4 = GetDescendant(input3, new String("NNP"), true);

		long s005 = System.currentTimeMillis();
		ArrayList predict1 = FilterInput1byInput2(input1, input4);
		long s006 = System.currentTimeMillis();
		ArrayList input5 = GetDescendant(predict1, new String("S"), true);
		long s007 = System.currentTimeMillis();
		ArrayList input6 = GetDescendant(input5, new String("PP"), true);
		long s008 = System.currentTimeMillis();
		ArrayList input7 = GetDescendant(input6, new String("JJ"), true);
		long s009 = System.currentTimeMillis();
		ArrayList predict2 = FilterInput1byInput2(input5, input7);
		long s010 = System.currentTimeMillis();
		ArrayList input8 = GetDescendant(predict2, new String("VBN"), true);
		long s011 = System.currentTimeMillis();
		ArrayList predict3 = FilterInput1byInput2(predict1, input8);
		long s012 = System.currentTimeMillis();
		ArrayList input9 = GetDescendant(predict3, new String("PP"), true);
		long s013 = System.currentTimeMillis();
		ArrayList input10 = GetChild(input9, new String("NP"), true);
		long s014 = System.currentTimeMillis();
		ArrayList input11 = GetDescendant(input10, new String("_NONE_"), true);
		long s015 = System.currentTimeMillis();
		ArrayList last = input11;
		ArrayList result = new ArrayList();
		int len = last.size();

		if (len != 0) {
			Collections.sort(last);

			int tmp = ((Integer) last.get(0)).intValue();
			result.add(new Integer(tmp));
			for (int i = 1; i < len; i++) {
				int next = ((Integer) last.get(i)).intValue();
				if (tmp != next) {
					result.add(new Integer(next));
					tmp = next;
				}
			}
		}
		long s016 = System.currentTimeMillis();
		System.err.println("\n\n\n@@@%%%%%%%%%%T3 The result count= " + result.size());

		System.out.println(
				"\n############Test_T3  <<<<<<<<<<<<");
		System.out.print("input_doc=" + input_doc.size() + ";  R_num=" + GetRelationNum(input_doc));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s002 - s001));

		System.out.print("input1=" + input1.size() + ";  R_num=" + GetRelationNum(input1));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s003 - s002));

		System.out.print("input2=" + input2.size() + ";  R_num=" + GetRelationNum(input2));
		System.out.println("\t\t GetChild--->used time(ms): " + (s004 - s003));

		System.out.print("input3=" + input3.size() + ";  R_num=" + GetRelationNum(input3));
		System.out.println("\t\t pppp_GetDescendant--->used time(ms): " + (s005 - s004));

		System.out.print("input1=" + input1.size() + ";  R_num=" + GetRelationNum(input1) + "   &&   input4="
				+ input4.size() + ";  R_num=" + GetRelationNum(input4));
		System.out.println("\t\t FilterInput1byInput2--->used time(ms): " + (s006 - s005));

		System.out.print("predict1=" + predict1.size() + ";  R_num=" + GetRelationNum(predict1));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s007 - s006));

		System.out.print("input5=" + predict1.size() + ";  R_num=" + GetRelationNum(input5));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s008 - s007));

		System.out.print("input6=" + input6.size() + ";  R_num=" + GetRelationNum(input6));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s009 - s008));

		System.out.print("input5=" + input5.size() + ";  R_num=" + GetRelationNum(input5) + "   &&   input7="
				+ input7.size() + ";  R_num=" + GetRelationNum(input7));
		System.out.println("\t\t FilterInput1byInput2--->used time(ms): " + (s010 - s009));

		System.out.print("predict2=" + predict2.size() + ";  R_num=" + GetRelationNum(predict2));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s011 - s010));

		System.out.print("predict1=" + predict1.size() + ";  R_num=" + GetRelationNum(predict1) + "   &&   input8="
				+ input8.size() + ";  R_num=" + GetRelationNum(input8));
		System.out.println("\t\t FilterInput1byInput2--->used time(ms): " + (s012 - s011));

		System.out.print("predict3=" + predict3.size() + ";  R_num=" + GetRelationNum(predict3));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s013 - s012));

		System.out.print("input9=" + input9.size() + ";  R_num=" + GetRelationNum(input9));
		System.out.println("\t\t GetChild--->used time(ms): " + (s014 - s013));

		System.out.print("input10=" + input10.size() + ";  R_num=" + GetRelationNum(input10));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s015 - s014));

		System.out.println("\t\t \u9664\u91CD--->used time(ms): " + (s016 - s015));

		System.out.println("\t\t @@@Query total time(ms): " + (s016 - s001));
		System.out.println(">>>>>>>>>>>>>");
	}

	void Test_T4() {
		ArrayList input_doc = new ArrayList();
		input_doc.add(Integer.valueOf(0));
		long s001 = System.currentTimeMillis();
		ArrayList input1 = GetDescendant(input_doc, new String("EMPTY"), true);
		long s002 = System.currentTimeMillis();
		ArrayList input2 = GetChild(input1, new String("_PERIOD_"), true);
		long s003 = System.currentTimeMillis();
		ArrayList input3 = GetChild(input1, new String("S"), true);
		long s004 = System.currentTimeMillis();
		ArrayList input4 = GetChild(input3, new String("VBP"), true);
		long s005 = System.currentTimeMillis();
		ArrayList predict1 = FilterInput1byInput2(input3, input4);
		long s006 = System.currentTimeMillis();
		ArrayList input5 = GetDescendant(predict1, new String("TO"), true);
		long s007 = System.currentTimeMillis();

		ArrayList predict2 = FilterInput1byInput2_OR(input1, input2, input5);

		long s008 = System.currentTimeMillis();

		ArrayList last = predict2;
		ArrayList result = new ArrayList();
		int len = last.size();

		if (len != 0) {
			Collections.sort(last);

			int tmp = ((Integer) last.get(0)).intValue();
			result.add(new Integer(tmp));
			for (int i = 1; i < len; i++) {
				int next = ((Integer) last.get(i)).intValue();
				if (tmp != next) {
					result.add(new Integer(next));
					tmp = next;
				}
			}
		}
		long s009 = System.currentTimeMillis();
		System.err.println("\n\n\n@@@ T4 The result count= " + result.size());

		System.out.println(
				"\n############Test_T4 <<<<<<<<<<<<");
		System.out.print("input_doc=" + input_doc.size() + ";  R_num=" + GetRelationNum(input_doc));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s002 - s001));
		System.out.print("input1=" + input1.size() + ";  R_num=" + GetRelationNum(input1));
		System.out.println("\t\t GetChild--->used time(ms): " + (s003 - s002));
		System.out.print("input1=" + input1.size() + ";  R_num=" + GetRelationNum(input1));
		System.out.println("\t\t GetChild--->used time(ms): " + (s004 - s003));
		System.out.print("input3=" + input3.size() + ";  R_num=" + GetRelationNum(input3));
		System.out.println("\t\t GetChild--->used time(ms): " + (s005 - s004));
		System.out.print("input3=" + input3.size() + ";  R_num=" + GetRelationNum(input3) + "   &&   input4="
				+ input4.size() + ";  R_num=" + GetRelationNum(input4));
		System.out.println("\t\t FilterInput1byInput2--->used time(ms): " + (s006 - s005));
		System.out.print("predict1=" + predict1.size() + ";  R_num=" + GetRelationNum(predict1));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s007 - s006));
		System.out.print("input1=" + input1.size() + ";  R_num=" + GetRelationNum(input1) + "   &&   input2="
				+ input2.size() + "   &&   input5=" + input5.size());
		System.out.println("\t\t FilterInput1byInput2_OR--->used time(ms): " + (s008 - s007));

		System.out.println("\t\t Remove duplication--->used time(ms): " + (s009 - s008));

		System.out.println("\t\t @@@Query total time(ms): " + (s009 - s001));
		System.out.println(">>>>>>>>>>>>>");
	}

	void Test_X1() {
		ArrayList input_doc = new ArrayList();
		input_doc.add(Integer.valueOf(0));
		long s001 = System.currentTimeMillis();

		ArrayList input1 = GetDescendant_parallel_new(input_doc, new String("open_auctions"), true, TASK_NUMBER);

		long s002 = System.currentTimeMillis();
		ArrayList input2 = GetChild(input1, new String("open_auction"), true);

		long s003 = System.currentTimeMillis();
		ArrayList input3 = GetDescendant(input2, new String("time"), true);
		long s004 = System.currentTimeMillis();

		ArrayList last = input3;
		ArrayList result = new ArrayList();
		int len = last.size();

		if (len != 0) {
			Collections.sort(last);

			int tmp = ((Integer) last.get(0)).intValue();
			result.add(new Integer(tmp));
			for (int i = 1; i < len; i++) {
				int next = ((Integer) last.get(i)).intValue();
				if (tmp != next) {
					result.add(new Integer(next));
					tmp = next;
				}
			}
		}
		long s005 = System.currentTimeMillis();

		System.err.println("\n $$$$$ Test_X1 The result count= " + result.size());

		System.out.println(
				"\n############Test_X1 <<<<<<<<<<");
		System.out.print("input_doc=" + input_doc.size() + ";  R_num=" + GetRelationNum(input_doc));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s002 - s001));

		System.out.print("input1=" + input1.size() + ";  R_num=" + GetRelationNum(input1));
		System.out.println("\t\t GetChild--->used time(ms): " + (s003 - s002));

		System.out.print("input2=" + input2.size() + ";  R_num=" + GetRelationNum(input2));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s004 - s003));

		System.out.println("\t\t Remove duplication-->used time(ms): " + (s005 - s004));

		System.out.println("\t\t @@@Query total time(ms): " + (s005 - s001));
		System.out.println(">>>>>>>>>>>>>");
	}

	void Test_X2() {
		ArrayList input_doc = new ArrayList();
		input_doc.add(Integer.valueOf(0));
		long s001 = System.currentTimeMillis();

		ArrayList input1 = GetDescendant_parallel_new(input_doc, new String("regions"), true, TASK_NUMBER);

		long s002 = System.currentTimeMillis();
		ArrayList input2 = GetChild(input1, new String("asia"), true);

		long s003 = System.currentTimeMillis();
		ArrayList input3 = GetChild(input2, new String("item"), true);
		long s004 = System.currentTimeMillis();
		ArrayList input4 = GetChild(input3, new String("payment"), true);
		long s005 = System.currentTimeMillis();
		ArrayList predict1 = FilterInput1byInput2(input3, input4);
		long s006 = System.currentTimeMillis();
		ArrayList input5 = GetDescendant(predict1, new String("name"), true);
		long s007 = System.currentTimeMillis();

		ArrayList last = input5;
		ArrayList result = new ArrayList();
		int len = last.size();

		if (len != 0) {
			Collections.sort(last);

			int tmp = ((Integer) last.get(0)).intValue();
			result.add(new Integer(tmp));
			for (int i = 1; i < len; i++) {
				int next = ((Integer) last.get(i)).intValue();
				if (tmp != next) {
					result.add(new Integer(next));
					tmp = next;
				}
			}
		}
		long s008 = System.currentTimeMillis();

		System.err.println("\n $$$$$ Test_X2 The result count= " + result.size());

		System.out.println(
				"\n############Test_X3 <<<<<<<<<<<<");
		System.out.print("input_doc=" + input_doc.size() + ";  R_num=" + GetRelationNum(input_doc));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s002 - s001));

		System.out.print("input1=" + input1.size() + ";  R_num=" + GetRelationNum(input1));
		System.out.println("\t\t GetChild--->used time(ms): " + (s003 - s002));

		System.out.print("input2=" + input2.size() + ";  R_num=" + GetRelationNum(input2));
		System.out.println("\t\t GetChild--->used time(ms): " + (s004 - s003));

		System.out.print("input3=" + input3.size() + ";  R_num=" + GetRelationNum(input3));
		System.out.println("\t\t GetChild--->used time(ms): " + (s005 - s004));

		System.out.print("input3=" + input3.size() + ";  R_num=" + GetRelationNum(input3) + "   &&   input4="
				+ input4.size() + ";  R_num=" + GetRelationNum(input4));
		System.out.println("\t\t FilterInput1byInput2--->used time(ms): " + (s006 - s005));

		System.out.print("predict1=" + predict1.size() + ";  R_num=" + GetRelationNum(predict1));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s007 - s006));

		System.out.println("\t\t Remove duplication--->used time(ms): " + (s008 - s007));

		System.out.println("\t\t @@@Query total time(ms): " + (s008 - s001));
		System.out.println(">>>>>>>>>>>>>");
	}

	void Test_X3() {
		ArrayList input_doc = new ArrayList();
		input_doc.add(Integer.valueOf(0));
		long s001 = System.currentTimeMillis();

		ArrayList input1 = GetDescendant(input_doc, new String("people"), true);
		input1 = GetDescendant_parallel_new(input_doc, new String("people"), true, TASK_NUMBER);

		long s002 = System.currentTimeMillis();

		ArrayList input2 = GetChild(input1, new String("person"), true);
		long s003 = System.currentTimeMillis();

		ArrayList input3 = GetDescendant(input2, new String("emailaddress"), true);
		long s004 = System.currentTimeMillis();

		ArrayList predict1 = FilterInput1byInput2(input2, input3);
		long s005 = System.currentTimeMillis();

		ArrayList input4 = GetDescendant(predict1, new String("creditcard"), true);
		long s006 = System.currentTimeMillis();

		ArrayList predict2 = FilterInput1byInput2(predict1, input4);

		long s007 = System.currentTimeMillis();

		ArrayList last = predict2;
		ArrayList result = new ArrayList();
		int len = last.size();

		if (len != 0) {
			Collections.sort(last);

			int tmp = ((Integer) last.get(0)).intValue();
			result.add(new Integer(tmp));
			for (int i = 1; i < len; i++) {
				int next = ((Integer) last.get(i)).intValue();
				if (tmp != next) {
					result.add(new Integer(next));
					tmp = next;
				}
			}
		}
		long s008 = System.currentTimeMillis();

		System.err.println("\n $$$$$ Test_X3 The result count= " + result.size());

		System.out.println(
				"\n############Test_X3 <<<<<<<<<<<<");
		System.out.print("input_doc=" + input_doc.size() + ";  R_num=" + GetRelationNum(input_doc));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s002 - s001));

		System.out.print("input1=" + input1.size() + ";  R_num=" + GetRelationNum(input1));
		System.out.println("\t\t GetChild--->used time(ms): " + (s003 - s002));

		System.out.print("input2=" + input2.size() + ";  R_num=" + GetRelationNum(input2));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s004 - s003));

		System.out.print("input2=" + input2.size() + ";  R_num=" + GetRelationNum(input2) + "   &&   input3="
				+ input3.size() + ";  R_num=" + GetRelationNum(input3));
		System.out.println("\t\t FilterInput1byInput2--->used time(ms): " + (s005 - s004));

		System.out.print("predict1=" + predict1.size() + ";  R_num=" + GetRelationNum(predict1));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s006 - s005));

		System.out.print("predict1=" + predict1.size() + ";  R_num=" + GetRelationNum(predict1) + "   &&   input4="
				+ input4.size() + ";  R_num=" + GetRelationNum(input4));
		System.out.println("\t\t FilterInput1byInput2--->used time(ms): " + (s007 - s006));

		System.out.println("\t\t Remove duplication--->used time(ms): " + (s008 - s007));

		System.out.println("\t\t @@@Query total time(ms): " + (s008 - s001));
		System.out.println(">>>>>>>>>>>>>");
	}

	void Test_X4() {
		ArrayList input_doc = new ArrayList();
		input_doc.add(Integer.valueOf(0));
		long s001 = System.currentTimeMillis();

		ArrayList input1 = GetDescendant_parallel_new(input_doc, new String("categories"), true, TASK_NUMBER);
		long s002 = System.currentTimeMillis();

		ArrayList input2 = GetChild(input1, new String("category"), true);
		long s003 = System.currentTimeMillis();

		ArrayList input3 = GetChild(input2, new String("name"), true);
		long s004 = System.currentTimeMillis();

		ArrayList predict1 = FilterInput1byInput2(input2, input3);
		long s005 = System.currentTimeMillis();

		ArrayList input4 = GetChild_attribute(predict1, new String("id"), true);
		long s006 = System.currentTimeMillis();

		ArrayList predict2 = FilterInput1byInput2_attribute(input2, input4);
		long s007 = System.currentTimeMillis();

		ArrayList predict3 = FilterInput1byInput2(input1, predict2);
		long s008 = System.currentTimeMillis();

		ArrayList input5 = GetDescendant(predict3, new String("description"), true);
		long s009 = System.currentTimeMillis();

		ArrayList last = input5;
		ArrayList result = new ArrayList();
		int len = last.size();

		if (len != 0) {
			Collections.sort(last);

			int tmp = ((Integer) last.get(0)).intValue();
			result.add(new Integer(tmp));
			for (int i = 1; i < len; i++) {
				int next = ((Integer) last.get(i)).intValue();
				if (tmp != next) {
					result.add(new Integer(next));
					tmp = next;
				}
			}
		}
		long s010 = System.currentTimeMillis();

		System.err.println("\n $$$$$ Test_X4 The result count= " + result.size());

		System.out.println(
				"\n############Test_X4  <<<<<<<<<<<<");
		System.out.print("input_doc=" + input_doc.size() + ";  R_num=" + GetRelationNum(input_doc));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s002 - s001));

		System.out.print("input1=" + input1.size() + ";  R_num=" + GetRelationNum(input1));
		System.out.println("\t\t GetChild--->used time(ms): " + (s003 - s002));

		System.out.print("input2=" + input2.size() + ";  R_num=" + GetRelationNum(input2));
		System.out.println("\t\t GetChild--->used time(ms): " + (s004 - s003));

		System.out.print("input2=" + input2.size() + ";  R_num=" + GetRelationNum(input2) + "   &&   input3="
				+ input3.size() + ";  R_num=" + GetRelationNum(input3));
		System.out.println("\t\t FilterInput1byInput2--->used time(ms): " + (s005 - s004));

		System.out.print("predict1=" + predict1.size() + ";  R_num=" + GetRelationNum(predict1));
		System.out.println("\t\t GetChild_attribute--->used time(ms): " + (s006 - s005));

		System.out.print("input2=" + input2.size() + ";  R_num=" + GetRelationNum(input2) + "   &&   input4="
				+ input4.size() + ";  R_num=" + GetRelationNum(input4));
		System.out.println("\t\t FilterInput1byInput2_attribute--->used time(ms): " + (s007 - s006));

		System.out.print("input1=" + input1.size() + ";  R_num=" + GetRelationNum(input1) + "   &&   predict2="
				+ predict2.size() + ";  R_num=" + GetRelationNum(predict2));
		System.out.println("\t\t FilterInput1byInput2--->used time(ms): " + (s008 - s007));

		System.out.print("predict3=" + predict3.size() + ";  R_num=" + GetRelationNum(predict3));
		System.out.println("\t\t GetDescendant--->used time(ms): " + (s009 - s008));

		System.out.println("\t\t Remove duplication--->used time(ms): " + (s010 - s009));

		System.out.println("\t\t @@@Query total time(ms): " + (s010 - s001));
		System.out.println(">>>>>>>>>>>>>");
	}

	private int GetRelationNum(ArrayList<Integer> input) {
		int num = 0;
		Iterator it = input.iterator();
		while (it.hasNext()) {
			int id = ((Integer) it.next()).intValue();
			num += ((RegionEncoding) this.rc.get(id)).getContained_number_save();
		}
		return num;
	}

}

class Position {
	int i;
	int j;

	Position() {
	}

	Position(int i, int j) {
		this.i = i;
		this.j = j;
	}
}