package JavaObjectPersistency.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Query implements Filter {
    private Filter rootFilter;
    private final String queryString;
    private static final ObjectMapper mapper = new ObjectMapper();

    public Query(String query) {
        this.queryString = query;
        this.rootFilter = parseQuery(query);
    }

    private Filter parseQuery(String query) {
        // Check for balanced parentheses and try to auto-correct minor issues
        query = balanceParentheses(query.trim());
        return buildExpressionTree(query);
    }

    private String balanceParentheses(String expression) {
        // Count parentheses and try to auto-correct common issues
        int openCount = 0;
        int closeCount = 0;

        for (char c : expression.toCharArray()) {
            if (c == '(') openCount++;
            else if (c == ')') closeCount++;
        }

        // Trim excess closing parentheses at the end
        if (closeCount > openCount) {
            int excess = closeCount - openCount;
            if (expression.endsWith(")".repeat(excess))) {
                return expression.substring(0, expression.length() - excess);
            }
        }

        return expression;
    }

    private Filter buildExpressionTree(String expression) {
        ExpressionParser parser = new ExpressionParser(expression);
        return parser.parse();
    }

    @Override
    public boolean matches(JsonNode node) {
        return rootFilter != null && rootFilter.matches(node);
    }

    public boolean validateObject(String serializedJsonString) {
        try {
            JsonNode node = mapper.readTree(serializedJsonString);
            return matches(node);
        } catch (Exception e) {
            return false;
        }
    }

    // Inner class for parsing expressions
    private class ExpressionParser {
        private final String expression;
        private int position = 0;

        public ExpressionParser(String expression) {
            this.expression = expression.trim();
        }

        public Filter parse() {
            Filter result = parseExpression();

            // Skip any trailing whitespace
            skipWhitespace();

            // More tolerant of trailing characters, just log them as warnings
            if (position < expression.length()) {
                System.out.println("Warning: Ignoring trailing characters in query: " +
                        expression.substring(position));
            }

            return result;
        }

        private Filter parseExpression() {
            Filter left = parseTerm();

            while (position < expression.length()) {
                skipWhitespace();
                if (position >= expression.length() ||
                        !matchKeyword("OR")) {
                    break;
                }
                // Skip "OR"
                position += 2;
                Filter right = parseTerm();
                left = new OrFilter(left, right);
            }

            return left;
        }

        private Filter parseTerm() {
            Filter left = parseFactor();

            while (position < expression.length()) {
                skipWhitespace();
                if (position >= expression.length() ||
                        !matchKeyword("AND")) {
                    break;
                }
                // Skip "AND"
                position += 3;
                Filter right = parseFactor();
                left = new AndFilter(left, right);
            }

            return left;
        }

        private boolean matchKeyword(String keyword) {
            // Check if the keyword appears at the current position
            int remainingLength = expression.length() - position;
            if (remainingLength < keyword.length()) return false;

            String potentialKeyword = expression.substring(position, position + keyword.length());
            // Check exact match or followed by whitespace or opening parenthesis
            if (potentialKeyword.equalsIgnoreCase(keyword)) {
                if (potentialKeyword.length() == remainingLength ||
                        !Character.isLetterOrDigit(expression.charAt(position + keyword.length()))) {
                    return true;
                }
            }
            return false;
        }

        private Filter parseFactor() {
            skipWhitespace();

            // Check for NOT operator
            boolean isNegated = false;
            if (matchKeyword("NOT")) {
                isNegated = true;
                position += 3; // Skip "NOT"
                skipWhitespace();
            }

            // Parse the condition or sub-expression
            Filter filter;

            if (position < expression.length() && expression.charAt(position) == '(') {
                position++; // Skip opening parenthesis
                skipWhitespace();

                // Check if this is a field.condition format
                int dotPos = expression.indexOf('.', position);
                if (dotPos > position && dotPos < expression.length()) {
                    String field = expression.substring(position, dotPos).trim();
                    position = dotPos + 1;

                    // Parse the condition name
                    skipWhitespace();
                    int openParenPos = findNextNonEscaped('(', position);
                    if (openParenPos > position) {
                        String condition = expression.substring(position, openParenPos).trim();
                        position = openParenPos + 1;

                        // Parse the parameter value
                        int closeParenPos = findMatchingClosingParenthesis(openParenPos);
                        if (closeParenPos > openParenPos) {
                            String value = expression.substring(openParenPos + 1, closeParenPos).trim();
                            value = value.replace("'", "").replace("\"", "");
                            position = closeParenPos + 1;

                            filter = createFilter(field, condition, value);

                            // Skip closing parenthesis of the entire condition
                            skipWhitespace();
                            if (position < expression.length() && expression.charAt(position) == ')') {
                                position++;
                            }
                        } else {
                            throw new IllegalArgumentException("Missing closing parenthesis for condition parameter");
                        }
                    } else {
                        throw new IllegalArgumentException("Invalid condition format at position " + position);
                    }
                } else {
                    // This is a parenthesized expression
                    filter = parseExpression();

                    // Expect closing parenthesis
                    skipWhitespace();
                    if (position < expression.length() && expression.charAt(position) == ')') {
                        position++; // Skip closing parenthesis
                    } else {
                        throw new IllegalArgumentException("Missing closing parenthesis for sub-expression");
                    }
                }
            } else {
                throw new IllegalArgumentException("Expected condition or sub-expression at position " + position);
            }

            // Apply NOT operator if present
            return isNegated ? new NotFilter(filter) : filter;
        }

        private int findNextNonEscaped(char target, int startPos) {
            for (int i = startPos; i < expression.length(); i++) {
                if (expression.charAt(i) == target && (i == 0 || expression.charAt(i - 1) != '\\')) {
                    return i;
                }
            }
            return -1;
        }

        private int findMatchingClosingParenthesis(int openParenPos) {
            int depth = 1;
            for (int i = openParenPos + 1; i < expression.length(); i++) {
                if (expression.charAt(i) == '(' && (i == 0 || expression.charAt(i - 1) != '\\')) {
                    depth++;
                } else if (expression.charAt(i) == ')' && (i == 0 || expression.charAt(i - 1) != '\\')) {
                    depth--;
                    if (depth == 0) {
                        return i;
                    }
                }
            }
            return -1;
        }

        private void skipWhitespace() {
            while (position < expression.length() && Character.isWhitespace(expression.charAt(position))) {
                position++;
            }
        }
    }

    private Filter createFilter(String field, String condition, String value) {
        switch (condition.toLowerCase()) {
            case "equals":
                return new EqualsFilter(field, value);
            case "greaterthan":
                return new GreaterThanFilter(field, Double.parseDouble(value));
            case "lessthan":
                return new LessThanFilter(field, Double.parseDouble(value));
            case "contains":
                return new ContainsFilter(field, value);
            default:
                throw new IllegalArgumentException("Unknown condition: " + condition);
        }
    }
}
