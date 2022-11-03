package com.nowcoder.communnity.util;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveFilter {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);

    // 替换符
    private static final String REPLACEMENT = "***";

    // 根节点
    private TrieNode rootNode = new TrieNode();

    // 初始化
    @PostConstruct // 当当前容器被构建时，调用该函数
    public void init() {
        System.out.println("初始化函数被调用");
        try (
                InputStream io = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(io));
                )
        {
            String keyword;
            while ((keyword = reader.readLine()) != null) {
                this.addKeyword(keyword);
            }
        } catch (IOException e) {
            logger.error("加载敏感词文件失败！" + e.getMessage());
        }
    }

    // 将一个敏感词添加进前缀树中
    private void addKeyword(String keyword) {

        TrieNode tempNode = rootNode;
        for (int i = 0; i < keyword.length(); i++) {
            char c = keyword.charAt(i);
            TrieNode subNode = tempNode.getSubNode(c);

            if(subNode == null) {
                subNode = new TrieNode();
                tempNode.addSubNode(c, subNode);
            }

            tempNode = subNode;

            // 如果是最后一个字符，设置结束标识
            if (i == keyword.length() - 1) {
                tempNode.setKeywordEnd(true);
            }
        }
    }

    /**
     * 过滤敏感词
     *
     * @param text 待过滤的文本
     * @return 过滤后的问题
     */
    public String filter (String text) {

        if (StringUtils.isBlank(text)) {
            return null;
        }

        TrieNode tempNode = rootNode;
        int begin = 0;
        int position = 0;

        StringBuilder sb = new StringBuilder();

        while(position < text.length()) {

            char c = text.charAt(position);

            // 跳过符号
            if (isSymbol(c)) {
                if (tempNode == rootNode) {
                    sb.append(c);
                    begin++;
                }
                position++;
                continue;  // 跳过下面的处理
            }

            // 检查下节点
            tempNode = tempNode.getSubNode(c);
            // 以begin开头的字符串不是敏感词，因为下级节点没有了
            if (tempNode == null) {
                sb.append(text.charAt(begin));
                position = ++begin;
                tempNode = rootNode;
            } else if (tempNode.isKeywordEnd) {   // 结束标志，表示begin到position的字符是敏感词 替换掉
                sb.append(REPLACEMENT);
                begin = ++position;
                tempNode = rootNode;
            }else {  // 该位置还在敏感词的树中，继续向下检查
                position ++;
            }
        }

        // 最后一批字符计入结果
        sb.append(text.substring(begin));
        return sb.toString();

    }

    // 判断是否为符号
    private boolean isSymbol(Character c) {
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);
    }

    // 前缀树
    private class TrieNode {

        // 关键词结束标识
        private boolean isKeywordEnd = false;

        // 子节点 下级字符和下级节点
        private Map<Character, TrieNode> subNodes = new HashMap<>();

        public boolean isKeywordEnd() {
            return isKeywordEnd;
        }

        public void setKeywordEnd(boolean keywordEnd) {
            isKeywordEnd = keywordEnd;
        }

        public TrieNode getSubNode(Character c) {
            return subNodes.get(c);
        }

        public void addSubNode(Character c, TrieNode node) {
            subNodes.put(c, node);
        }
    }

}
