package io.modelcontextprotocol.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A utility class for validating and matching URI templates.
 * <p>
 * This class provides methods to validate the syntax of URI templates and check if a
 * given URI matches a specified template.
 * </p>
 */
public class UriTemplate {

    // Constants for security and performance limits
    private static final int MAX_TEMPLATE_LENGTH = 1_000_000;

    private static final int MAX_VARIABLE_LENGTH = 1_000_000;

    private static final int MAX_TEMPLATE_EXPRESSIONS = 10_000;

    private static final int MAX_REGEX_LENGTH = 1_000_000;

    private final String template;

    private final Pattern pattern;

    public String getTemplate() {
        return template;
    }

    /**
     * Constructor to create a new UriTemplate instance. Validates the template length,
     * parses it into parts, and compiles a regex pattern.
     *
     * @param template The URI template string
     * @throws IllegalArgumentException if the template is invalid or too long
     */
    public UriTemplate(String template) {
        if (template == null || Utils.isBlank(template)) {
            throw new IllegalArgumentException("Template cannot be null or empty");
        }
        validateLength(template, MAX_TEMPLATE_LENGTH, "Template");

        this.template = template;
        final List<Part> parts = parseTemplate(template);
        this.pattern = Pattern.compile(createMatchingPattern(parts));
    }

    /**
     * Returns the original template string.
     */
    @Override
    public String toString() {
        return template;
    }

    /**
     * Checks if a given URI matches the compiled template pattern.
     *
     * @param uri The URI to check
     * @return true if the URI matches the template pattern, false otherwise
     */
    public boolean matchesTemplate(String uri) {
        validateLength(uri, MAX_TEMPLATE_LENGTH, "URI");
        return pattern.matcher(uri).matches();
    }

    /**
     * Validates that a string does not exceed a maximum allowed length.
     *
     * @param str     String to validate
     * @param max     Maximum allowed length
     * @param context Context description for error message
     * @throws IllegalArgumentException if the string exceeds the maximum length
     */
    private static void validateLength(String str, int max, String context) {
        if (str.length() > max) {
            throw new IllegalArgumentException(
                    context + " exceeds maximum length of " + max + " characters (got " + str.length() + ")");
        }
    }

    /**
     * Parses the URI template into a list of parts (literals and expressions).
     *
     * @param template The URI template string
     * @return List of parts
     */
    private List<Part> parseTemplate(String template) {
        List<Part> parts = new ArrayList<>();
        StringBuilder literal = new StringBuilder();
        int expressionCount = 0;
        int index = 0;

        while (index < template.length()) {
            char ch = template.charAt(index);
            if (ch == '{') {
                if (literal.length() > 0) {
                    parts.add(new LiteralPart(literal.toString()));
                    literal.setLength(0);
                }
                int end = template.indexOf('}', index);
                if (end == -1) {
                    throw new IllegalArgumentException("Unclosed template expression");
                }
                expressionCount++;
                if (expressionCount > MAX_TEMPLATE_EXPRESSIONS) {
                    throw new IllegalArgumentException("Too many template expressions");
                }
                String expr = template.substring(index + 1, end);
                parts.add(parseExpression(expr));
                index = end + 1;
            } else {
                literal.append(ch);
                index++;
            }
        }
        if (literal.length() > 0) {
            parts.add(new LiteralPart(literal.toString()));
        }
        return parts;
    }

    /**
     * Parses a template expression into an ExpressionPart.
     *
     * @param expr The template expression string
     * @return An ExpressionPart representing the expression
     */
    private ExpressionPart parseExpression(String expr) {
        if (expr.trim().isEmpty()) {
            throw new IllegalArgumentException("Empty template expression");
        }
        String operator = extractOperator(expr);
        boolean exploded = expr.contains("*");
        List<String> names = extractNames(expr);
        if (names.isEmpty()) {
            throw new IllegalArgumentException("No variable names in template expression: " + expr);
        }
        names.forEach(name -> validateLength(name, MAX_VARIABLE_LENGTH, "Variable name"));
        return new ExpressionPart(operator, names, exploded);
    }

    /**
     * Extracts the operator from a template expression if present.
     *
     * @param expr The template expression string
     * @return The operator as a string, or an empty string if none
     */
    private String extractOperator(String expr) {
        switch (expr.charAt(0)) {
            case '+':
            case '#':
            case '.':
            case '/':
            case '?':
            case '&':
                return String.valueOf(expr.charAt(0));
            default:
                return "";
        }
    }

    /**
     * Extracts variable names from a template expression.
     *
     * @param expr The template expression string
     * @return A list of variable names
     */
    private List<String> extractNames(String expr) {
        String cleaned = expr.replaceAll("^[+.#/?&]", "");
        String[] nameParts = cleaned.split(",");
        List<String> names = new ArrayList<>();
        for (String part : nameParts) {
            String trimmed = part.replace("*", "").trim();
            if (!trimmed.isEmpty()) {
                names.add(trimmed);
            }
        }
        return names;
    }

    /**
     * Constructs a regex pattern string to match URIs based on the parsed template parts.
     *
     * @return A regex pattern string
     */
    private String createMatchingPattern(List<Part> parts) {
        StringBuilder patternBuilder = new StringBuilder("^");
        for (Part part : parts) {
            if (part instanceof ExpressionPart) {
                ExpressionPart exprPart = (ExpressionPart) part;
                patternBuilder.append(createPatternForExpressionPart(exprPart));
            } else if (part instanceof LiteralPart) {
                LiteralPart literalPart = (LiteralPart) part;
                patternBuilder.append(Pattern.quote(literalPart.getLiteral()));
            }
        }

        patternBuilder.append("$");
        String patternStr = patternBuilder.toString();
        validateLength(patternStr, MAX_REGEX_LENGTH, "Generated regex pattern");
        return patternStr;
    }

    /**
     * Creates a regex pattern for a specific expression part based on its operator.
     *
     * @param part The expression part
     * @return A regex pattern string
     */
    private String createPatternForExpressionPart(ExpressionPart part) {
        switch (part.getOperator()) {
            case "":
            case "+":
                return part.isExploded() ? "([^/]+(?:,[^/]+)*)" : "([^/,]+)";
            case "#":
                return "(.+)";
            case ".":
                return "\\.([^/,]+)";
            case "/":
                return "/" + (part.isExploded() ? "([^/]+(?:,[^/]+)*)" : "([^/,]+)");
            case "?":
            case "&":
                return "\\?" + part.getVariableNames().get(0) + "=([^&]+)";
            default:
                return "([^/]+)";
        }
    }

    // --- Internal types ---

    /**
     * A marker interface for parts of the URI template.
     */
    private interface Part {

    }

    /**
     * Represents a literal segment of the template.
     */
    private class LiteralPart implements Part {
        private final String literal;

        public LiteralPart(String literal) {
            this.literal = literal;
        }

        public String getLiteral() {
            return literal;
        }
    }

    /**
     * Represents an expression segment of the template.
     */
    private class ExpressionPart implements Part {
        private final String operator;
        private final List<String> variableNames;
        private final boolean exploded;

        public ExpressionPart(String operator, List<String> variableNames, boolean exploded) {
            this.operator = operator;
            this.variableNames = variableNames;
            this.exploded = exploded;
        }

        public String getOperator() {
            return operator;
        }

        public List<String> getVariableNames() {
            return variableNames;
        }

        public boolean isExploded() {
            return exploded;
        }
    }

    @Override
    public int hashCode() {
        return template.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof UriTemplate) {
            UriTemplate other = (UriTemplate) obj;
            return template.equals(other.template);
        } else {
            return false;
        }
    }
}