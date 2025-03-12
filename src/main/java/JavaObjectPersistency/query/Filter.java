package JavaObjectPersistency.query;

import com.fasterxml.jackson.databind.JsonNode;

public interface Filter {
    boolean matches(JsonNode node);
}
