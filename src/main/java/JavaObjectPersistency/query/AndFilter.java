package JavaObjectPersistency.query;

import com.fasterxml.jackson.databind.JsonNode;

class AndFilter implements Filter {
    private final Filter left;
    private final Filter right;

    public AndFilter(Filter left, Filter right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean matches(JsonNode node) {
        return left.matches(node) && right.matches(node);
    }
}