package org.asdtiang.sensitivewdfilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import org.asdtiang.sensitivewdfilter.util.BCConvert;

/**
 * 创建时间：2016年8月30日 下午3:01:12
 * 
 * 思路： 创建一个FilterSet，枚举了0~65535的所有char是否是某个敏感词开头的状态
 * 
 * 判断是否是 敏感词开头 | | 是 不是 获取头节点 OK--下一个字 然后逐级遍历，DFA算法
 * 
 * @author andy
 * @version 2.2
 */
public class SensitiveWordFilterDefaultImpl implements SensitiveWordFilter{

	private static final FilterSet FILTER_SET = new FilterSet(); // 存储首字

	private static  Map<Integer, WordNode> nodes = new HashMap<Integer, WordNode>(1024); // 存储节点
	private static final Set<Integer> STOPWD_SET = new HashSet<>(); // 停顿词
	private static  char SIGN = '*'; // 敏感词过滤替换

	@Override
	public void init(int hashMapSize) {
		nodes = new HashMap<>(hashMapSize);
	}

	@Override
	public void changeReplaceChar(char replaceChar) {
		SIGN = replaceChar;
	}

	//	static {
//		try {
//			init();
//		} catch (Exception e) {
//			// 加载失败
//		}
//	}
//
//	private static void init() {
//		// 获取敏感词
//		addSensitiveWord(readWordFromFile("wd.txt"));
//		addStopWord(readWordFromFile("stopwd.txt"));
//	}

	/**
	 * 增加敏感词
	 * 
	 * @param path
	 * @return
	 */
	public List<String> readWordFromFile(String path) {
		List<String> words;
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(SensitiveWordFilterDefaultImpl.class.getClassLoader().getResourceAsStream(path)));
			words = new ArrayList<String>(1200);
			for (String buf = ""; (buf = br.readLine()) != null;) {
				if (buf == null || buf.trim().equals(""))
					continue;
				words.add(buf);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
			}
		}
		return words;
	}

	/**
	 * 增加停顿词
	 * 
	 * @param words
	 */
	public void addStopWord(List<String> words) {
		if (!isEmpty(words)) {
			char[] chs;
			for (String curr : words) {
				chs = curr.toCharArray();
				for (char c : chs) {
					STOPWD_SET.add(charConvert(c));
				}
			}
		}
	}

	/**
	 * 添加DFA节点
	 * 
	 * @param words
	 */
	public  void addSensitiveWord(List<String> words) {
		if (!isEmpty(words)) {
			char[] chs;
			int fchar;
			int lastIndex;
			WordNode fnode; // 首字母节点
			for (String curr : words) {
				chs = curr.toCharArray();
				fchar = charConvert(chs[0]);
				if (!FILTER_SET.contains(fchar)) {// 没有首字定义
					FILTER_SET.add(fchar);// 首字标志位 可重复add,反正判断了，不重复了
					fnode = new WordNode(fchar, chs.length == 1);
					nodes.put(fchar, fnode);
				} else {
					fnode = nodes.get(fchar);
					if (!fnode.isLast() && chs.length == 1)
						fnode.setLast(true);
				}
				lastIndex = chs.length - 1;
				for (int i = 1; i < chs.length; i++) {
					fnode = fnode.addIfNoExist(charConvert(chs[i]), i == lastIndex);
				}
			}
		}
	}

	/**
	 * 过滤判断 将敏感词转化为成屏蔽词
	 * 
	 * @param src
	 * @return
	 */
	public   String doFilter( String src) {
		if (FILTER_SET != null && nodes != null) {
			char[] chs = src.toCharArray();
			int length = chs.length;
			int currc; // 当前检查的字符
			int cpcurrc; // 当前检查字符的备份
			int k;
			WordNode node;
			for (int i = 0; i < length; i++) {
				currc = charConvert(chs[i]);
				if (!FILTER_SET.contains(currc)) {
					continue;
				}
				node = nodes.get(currc);
				if (node == null) {
					continue;
				}
				boolean couldMark = false;
				int markNum = -1;
				if (node.isLast()) {// 单字匹配（日）
					couldMark = true;
					markNum = 0;
				}
				// 继续匹配（日你/日你妹），以长的优先
				// 你-3 妹-4 夫-5
				k = i;
				cpcurrc = currc; // 当前字符的拷贝
				for (; ++k < length;) {
					int temp = charConvert(chs[k]);
					if (temp == cpcurrc) {
						continue;
					}
					if (STOPWD_SET != null && STOPWD_SET.contains(temp)) {
						continue;
					}
					node = node.querySub(temp);
					if (node == null) {// 没有了
						break;
					}
					if (node.isLast()) {
						couldMark = true;
						markNum = k - i;// 3-2
					}
					cpcurrc = temp;
				}
				if (couldMark) {
					for (k = 0; k <= markNum; k++) {
						chs[k + i] = SIGN;
					}
					i = i + markNum;
				}
			}
			return new String(chs);
		}

		return src;
	}

	/**
	 * 是否包含敏感词
	 * 
	 * @param src
	 * @return
	 */
	public  boolean isContains(String src) {
		if (FILTER_SET != null && nodes != null) {
			char[] chs = src.toCharArray();
			int length = chs.length;
			int currc; // 当前检查的字符
			int cpcurrc; // 当前检查字符的备份
			int k;
			WordNode node;
			for (int i = 0; i < length; i++) {
				currc = charConvert(chs[i]);
				if (!FILTER_SET.contains(currc)) {
					continue;
				}
				node = nodes.get(currc);// 日 2
				if (node == null)// 其实不会发生，习惯性写上了
					continue;
				boolean couldMark = false;
				if (node.isLast()) {// 单字匹配（日）
					couldMark = true;
				}
				// 继续匹配（日你/日你妹），以长的优先
				// 你-3 妹-4 夫-5
				k = i;
				cpcurrc = currc;
				for (; ++k < length;) {
					int temp = charConvert(chs[k]);
					if (temp == cpcurrc)
						continue;
					if (STOPWD_SET != null && STOPWD_SET.contains(temp))
						continue;
					node = node.querySub(temp);
					if (node == null)// 没有了
						break;
					if (node.isLast()) {
						couldMark = true;
					}
					cpcurrc = temp;
				}
				if (couldMark) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * 大写转化为小写 全角转化为半角
	 * 
	 * @param src
	 * @return
	 */
	public static int charConvert(char src) {
		int r = BCConvert.qj2bj(src);
		return (r >= 'A' && r <= 'Z') ? r + 32 : r;
	}

	/**
	 * 判断一个集合是否为空
	 * 
	 * @param col
	 * @return
	 */
	private static <T> boolean isEmpty(final Collection<T> col) {
		if (col == null || col.isEmpty()) {
			return true;
		}
		return false;
	}
}
