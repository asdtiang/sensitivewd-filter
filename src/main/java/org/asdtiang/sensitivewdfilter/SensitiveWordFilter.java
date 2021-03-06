package org.asdtiang.sensitivewdfilter;

import java.util.Set;

/**
 * @author abel lee
 * @create 2020-08-19 16:02
 **/
public interface SensitiveWordFilter {
    /**
     * 设置hashMap初始化大小,如果没有设置，默认为1024,大小建议为敏感词数量的2倍
     *
     * @param hashMapSize
     */
    void init(int hashMapSize);

    /**
     * 清空敏感词和停词,以便重先初始化
     */
    void clear();

    /**
     * 默认char 为 *
     *
     * @param replaceChar
     */
    void changeReplaceChar(char replaceChar);

    Set<String> readWordFromFile(String path);

    void addStopWord(Set<String> words);

    void addSensitiveWord(Set<String> words);

    String doFilter(String src);

    boolean isContains(String src);
}
