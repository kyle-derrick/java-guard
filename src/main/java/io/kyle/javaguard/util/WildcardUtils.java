package io.kyle.javaguard.util;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/9/30 10:32
 */
public class WildcardUtils {
    public static String wildcardToRegex(String wildcard) {
        StringBuilder sb = new StringBuilder(wildcard.length() + (wildcard.length() >> 1));
        for (int i = 0; i < wildcard.length(); i++) {
            char c = wildcard.charAt(i);
            switch (c) {
                case '*':
                    sb.append(".*");
                    break;
                case '?':
                    sb.append('.');
                    break;
                case '.':
                case '^':
                case '$':
                case '+':
                case '{':
                case '}':
                case '[':
                case ']':
                case '(':
                case ')':
                case '|':
                case '\\':
                    sb.append('\\').append(c);
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }
}
