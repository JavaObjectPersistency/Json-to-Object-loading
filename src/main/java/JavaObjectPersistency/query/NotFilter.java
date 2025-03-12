package JavaObjectPersistency.query;

import com.fasterxml.jackson.databind.JsonNode;

class NotFilter implements Filter {
    private final Filter filter;

    public NotFilter(Filter filter) {
        this.filter = filter;
    }

    @Override
    public boolean matches(JsonNode node) {
        return !filter.matches(node);
    }
}