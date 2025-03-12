package JavaObjectPersistency.query;

import com.fasterxml.jackson.databind.JsonNode;

class ContainsFilter implements Filter {
    private final String field;
    private final String value;

    public ContainsFilter(String field, String value) {
        this.field = field;
        this.value = value;
    }

    @Override
    public boolean matches(JsonNode node) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || !fieldNode.isTextual()) return false;

        return fieldNode.asText().contains(value);
    }
}