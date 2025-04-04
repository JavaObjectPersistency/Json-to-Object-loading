package JavaObjectPersistency.query;

import com.fasterxml.jackson.databind.JsonNode;

class LessThanFilter implements Filter {
    private final String field;
    private final double value;

    public LessThanFilter(String field, double value) {
        this.field = field;
        this.value = value;
    }

    @Override
    public boolean matches(JsonNode node) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || !fieldNode.isNumber()) return false;

        return fieldNode.asDouble() < value;
    }
}