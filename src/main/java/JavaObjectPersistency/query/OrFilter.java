package JavaObjectPersistency.query;

import com.fasterxml.jackson.databind.JsonNode;

class OrFilter implements Filter {
    private final Filter left;
    private final Filter right;

    public OrFilter(Filter left, Filter right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean matches(JsonNode node) {
        return left.matches(node) || right.matches(node);
    }
}