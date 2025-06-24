package io.github.benxincaomu.oa.base.utils;

/**
 * 字符串生成
 */
public class StringGenerator {

    private StringGenerator(){}


    // 字符数组，包含0-9、a-z、A-Z
    private static final char[] CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private static final int LENGTH = CHARS.length;

    public static String generate(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(CHARS[(int) (Math.random() * LENGTH)]);
        }
        return sb.toString();
    }

    
}
