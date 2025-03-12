package JavaObjectPersistency.query;

import com.fasterxml.jackson.databind.JsonNode;

class EqualsFilter implements Filter {
    private final String field;
    private final String value;

    public EqualsFilter(String field, String value) {
        this.field = field;
        this.value = value;
    }

    @Override
    public boolean matches(JsonNode node) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null) return false;

        if (fieldNode.isTextual()) {
            return fieldNode.asText().equals(value);
        } else if (fieldNode.isNumber()) {
            try {
                double nodeValue = fieldNode.asDouble();
                double compareValue = Double.parseDouble(value);
                return nodeValue == compareValue;
            } catch (NumberFormatException e) {
                return false;
            }
        } else if (fieldNode.isBoolean()) {
            return fieldNode.asBoolean() == Boolean.parseBoolean(value);
        }

        return false;
    }
}