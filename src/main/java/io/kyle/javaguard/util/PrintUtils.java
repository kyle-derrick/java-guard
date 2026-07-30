package io.kyle.javaguard.util;

import org.apache.commons.lang3.StringUtils;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/10/08 10:32
 */
public class PrintUtils {
    private int latestLen = 0;

    public void printInline(String text, Object ...args) {
        printInline(String.format(text, args));
    }

    public void printInline(String text) {
        int latestL = latestLen;
        int length = text.length();
        if (latestL > length) {
            text += StringUtils.repeat(' ', (latestL - length)<<1);
        }
        latestLen = length;
        System.out.printf('\r' + text);
    }

    public void over() {
        System.out.print('\r' + StringUtils.repeat(' ', latestLen<<1) + '\r');
    }
}
