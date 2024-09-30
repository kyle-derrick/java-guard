package io.kyle.javaguard.filter;

import com.google.re2j.Pattern;
import io.kyle.javaguard.util.WildcardUtils;

import java.util.LinkedList;
import java.util.StringJoiner;

/**
 * @author kyle kyle_derrick@foxmail.com
 * 2024/9/30 10:10
 */
public class SimpleFilter implements Filter {
    private final LinkedList<String> regex = new LinkedList<>();
    private Pattern pattern;
    @Override
    public boolean filtrate(String path) {
        if (regex.isEmpty()) {
            return false;
        }
        return pattern == null ? compile().filtrate(path) : pattern.matcher(path).matches();
    }

    public SimpleFilter addWildcard(String wildcard) {
        this.regex.add(WildcardUtils.wildcardToRegex(wildcard));
        return this;
    }

    public SimpleFilter addRegex(String regex) {
        this.regex.add(regex);
        return this;
    }

    public SimpleFilter addExpr(String expr) {
        if (expr.length() > 2 && expr.charAt(1) == ':') {
            switch (expr.charAt(0)) {
                case 'w':
                    expr = expr.substring(1);
                    break;
                case 'r':
                    return addRegex(expr.substring(2));
                default:
            }
        }
        return addWildcard(expr);
    }

    public SimpleFilter compile() {
        StringJoiner sj = new StringJoiner("|", "^(", ")$");
        regex.forEach(sj::add);
        pattern = Pattern.compile(sj.toString());
        return this;
    }
}
