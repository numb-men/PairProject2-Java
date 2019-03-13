import java.io.*;
import java.util.*;

public class Lib{

    // 行数
    private static int lineNum = 0;

    // 字符数
    private static int charNum = 0;

    // 单词数：至少四个英文字母开头，不区分大小写
    private static int wordNum = 0;

    // 单词集合：<单词, 数目>
    private static Map<String, Integer> wordMap = null;

    // 排序好的单词集合
    private static List<Map.Entry<String, Integer>> wordList = null;

    // 字节数组
    private static byte[] titleBytes = null;
	private static byte[] abstractBytes = null;

    // 长度：字节数组长度
    private static int titleBytesLength = 0;
	private static int abstractBytesLength = 0;
	
	// 词组词数
	private static int phraseWordNum = 1;
	
	// 权重
	private static int titleWordWeight = 1;
	private static int abstractWordWeight = 1;
	
	// 单词所需最小开头字母数
	private static final int MIN_WORD_LETTER_NUM = 4;

	
    public static int getCharNum(){return charNum;}
    public static int getWordNum(){return wordNum;}
    public static int getLineNum(){return lineNum;}
    public static List<Map.Entry<String, Integer>> getSortedList(){return wordList;}


    public static void main(String[] args) {
        System.out.println(isSeparator((byte)'&'));
    }

	
    /**
     * 初始化构造
     */
    public Lib(byte[] tBytes, byte[] aBytes, int phraseWordNum, int titleWordWeight,
				int abstractWordWeight) throws Exception{
					
		if ((tBytes == null || tBytes.length == 0) || aBytes == null || aBytes.length == 0){
			throw new Exception("字节数组不能为空！");
		}
        this.titleBytes = tBytes;
		this.abstractBytes = aBytes;
		this.titleBytesLength = tBytes.length;
		this.abstractBytesLength = aBytes.length;
		this.phraseWordNum = phraseWordNum;
		this.titleWordWeight = titleWordWeight;
		this.abstractWordWeight = abstractWordWeight;
        this.wordMap = new TreeMap<String, Integer>();
    }

	/**
	 * 将titleBytes和abstractBytes都进行预处理
	 */
	public void preProccess(){
		preProccess_(this.titleBytes);
		preProccess_(this.abstractBytes);
	}
	
    /**
     * 预处理
     *      将大写字母转为小写字母
     *      计算字节数组中的字符数、包括空字符、//r//n算作一个字符
	 *      兼容\n换行
     *      计算字节数组包含的行数
	 *
	 * @param bytes 字节数组
     */
    static void preProccess_(byte[] bytes){
		int bytesLength = bytes.length;
        // 计算字符数、行数
        for (int i = 0; i < bytesLength; i ++){
            // 预处理，大写字母统一转为小写字母
            if (bytes[i] >= 65 && bytes[i] <= 90){
                bytes[i] += 32;
            }
            if (bytes[i] == 10){
                // 计算行数
                if (checkLine(bytes, i)){
                    lineNum ++;
                }
				// 当换行为\n时保证不遗漏字符
				if (i-1 >= 0 && bytes[i-1] != 13){
					charNum ++;
				}
            }else{
                charNum ++;
            }
        }
        // 注意最后一行不以回车结尾的情况，同样算作一行
        if (bytes[bytesLength-1] != 10){
            lineNum ++;
        }
    }
	
	/**
	 * 将titleBytes和abstractBytes都进行词收集，同时分配权重
	 */
	public static void collectWord(){
		collectWord_(titleBytes, titleWordWeight);
		collectWord_(abstractBytes, abstractWordWeight);
	}

    /**
     * 计算单词/词组数、并将单词/词组装入集合、统计个数
	 *
	 * @param bytes 字节数组
	 * @param wordWeight 权重
     */
    static void collectWord_(byte[] bytes, int wordWeight){
		int bytesLength = bytes.length;
        int checkWordResult = -1;
		String wordString = null;
        for (int i = 0; i < bytesLength; i ++){
            if (isLetter(bytes[i])){
                checkWordResult = checkWord(bytes, i, MIN_WORD_LETTER_NUM);
                if (checkWordResult > 0){
					if (phraseWordNum == 1){
						wordString = subBytesToString(bytes, i, checkWordResult);
					} else {
						wordString = checkPhrase(bytes, i, MIN_WORD_LETTER_NUM, phraseWordNum);
					}
					// System.out.println(wordString);
					
					if (wordString != null){
						// 如果构成词组，存入集合中
						if (wordMap.containsKey(wordString)){
							wordMap.put(wordString, wordMap.get(wordString)+(wordWeight));
						} else{
							wordMap.put(wordString, wordWeight);
						}
					}
                    wordNum ++;
                    // 跳转单词末尾
                    i = checkWordResult;
                } else{
                    // 不是单词，但是同样跳转到词末尾
                    i = - checkWordResult;
                }
                // System.out.println(checkWordResult);
            }
        }
    }

    /**
     * 按照单词频率排序
     */
    public static void sortWordMap(){
        wordList = new ArrayList<Map.Entry<String,Integer>>(wordMap.entrySet());
        Collections.sort(wordList, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> word1, 
								Map.Entry<String, Integer> word2) {
                return word2.getValue() - word1.getValue();
            }
        });
    }

    /**
     * 取出字节数组中的某一段转成String返回
     *
     * @param bytes 字节数组
     * @param start 开始下标
     * @param end 截止下标
     *
     * @return aWordString 截取转成的字符串
     */
    public static String subBytesToString(byte[] bytes, int start, int end){
        if (end > start){
            byte[] aWordByte = new byte[end-start];
            System.arraycopy(bytes, start, aWordByte, 0, end-start);
            return new String(aWordByte);
        }
        return null;
    }

    /**
     * 判断该换行字符所在行是否是非空白行
     *
     * @param bytes 字节数组
     * @param lineEnd 换行符下标（行末尾）
     *
     * @return true 非空行 fasle 是空行
     */
    static boolean checkLine(byte[] bytes, int lineEnd){
        int notBlankCharNum = 0;
        for (int i = lineEnd-1; i >= 0; i --){
            if (bytes[i] == 10){
                // 遇到前一行返回
                break;
            } else if (!isBlankChar(bytes[i])){
                // 当前字母不是空格或制表符
                notBlankCharNum ++;
            }
        }
        return (notBlankCharNum > 0);
    }

    /**
     * 判断byte字节是否是字母
     *
     * @param b 字节
     *
     * @return true 是字母 false 不是字母
    **/
    static boolean isLetter(byte b){
        return (b >= 97 && b <= 122) || (b >= 65 && b <= 90);
    }

    /**
     * 判断Byte字节是否是数字
     *
     * @param b 字节
     *
     * @return true 是数字 false 不是数字
     */
    static boolean isNum(byte b){
        return (b >= 48 && b <= 57);
    }

    /**
     * 判断byte字节是否是空白字符
     *
     * @param b 字节
     *
     * @return true 是空白字符 false 不是空白字符
    **/
    static boolean isBlankChar(byte b){
        return (b <= 32 || b == 127);
    }

    /**
     * 判断Byte字节是否是分隔符
     *
     * @param b 字节
     *
     * @return true 是分隔符 false 不是分隔符
     */
    static boolean isSeparator(byte b){
        return !(isLetter(b)|| isNum(b));
    }

	/**
	 * 判断从某个下标开始的一段长度是否能构成所要求的词组长度
	 *
	 * @param bytes 字节数组
     * @param start 开始下标
     * @param minWordLength 满足最小需求的开头字母数
	 * @param phraseWordNum 词组所需的词数
	 *
	 * @return null 不能构成词组（不合法字符或者词数不足）
	 *       String 能构成词组，返回词组
	 */
	static String checkPhrase(byte[] bytes, int start, int minWordLength, int phraseWordNum){
		int bytesLength = bytes.length;
		int nowIndex = start;
		int checkWordResult = -1;
		for (int i = 0; i < phraseWordNum; i++){
			checkWordResult = checkWord(bytes, nowIndex, MIN_WORD_LETTER_NUM);
			if (checkWordResult > 0){
				String aWordString = subBytesToString(bytes, nowIndex, checkWordResult);
				// System.out.println(aWordString);
				// 跳转单词末尾
				nowIndex = checkWordResult;
				// 判断是否是换行分隔符(\r\n或者\n)，如是，无法构成词组
				if ((i != phraseWordNum-1) && (bytes[nowIndex] == 10 || 
						(nowIndex < bytesLength-1 && bytes[nowIndex + 1] == 10))){
					// System.out.println("遇到换行");
					return null;
				}
				// 跳过分隔符
				if (i != phraseWordNum-1){
					nowIndex ++;
				}
			} else{
				// System.out.println("不是单词，无法构成词组");
				return null;
			}
		}
		// 满足构成单词条件，返回词组
		return subBytesToString(bytes, start, nowIndex);
	}
	
    /**
     * 判断从某个下标开始的一段长度是否是单词
     *
     * @param bytes 字节数组
     * @param start 开始下标
     * @param minWordLength 满足最小需求的开头字母数
     *
     * @return int < 0 不是单词，负的词末尾分隔符的下标
     *      int > 0 是单词，单词末尾分隔符的下标
    **/
    static int checkWord(byte[] bytes, int start, int minWordLength){
        int bytesLength = bytes.length;
        int i = start;
        int checkWordResult = 0;

        if (start > 0 && ! isSeparator(bytes[start-1])){
            checkWordResult = -1;
        } else{
            for (; i < start + minWordLength && i < bytesLength; i++){
                // 满足最小需求的开头字母数
                if (! isLetter(bytes[i])){
                    checkWordResult = -2;
                    break;
                }
            }
            // 已到结尾，不满足最小开头字母数
            if (i == bytesLength && i - start < minWordLength){
                checkWordResult = -3;
            }
        }
        for (; i < bytesLength; i++){
            // 遍历到词结束，返回词末尾的下标
            if (isSeparator(bytes[i])){
                // 字符不是分隔符
                break;
            }
        }

        return checkWordResult < 0 ? -i : i;
    }
}