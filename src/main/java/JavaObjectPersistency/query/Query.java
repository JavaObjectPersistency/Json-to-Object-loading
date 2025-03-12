package JavaObjectPersistency.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Query implements Filter {
    private final List<Filter> filters = new ArrayList<>();
    private final String queryString;
    private static final ObjectMapper mapper = new ObjectMapper();

    public Query(String query) {
        this.queryString = query;
        parseQuery(query);
    }

    private void parseQuery(String query) {
        // Паттерн для распознавания условий в формате: [NOT] (field.cond(val)) AND/OR [NOT] (field.cond(val))
        Pattern pattern = Pattern.compile("(?i)(NOT\\s+)?\\s*\\(([^.]+)\\.([^(]+)\\(([^)]+)\\)\\)\\s*(AND|OR)?");
        Matcher matcher = pattern.matcher(query);

        Filter currentFilter = null;
        String lastOperator = null;

        while (matcher.find()) {
            boolean isNegated = matcher.group(1) != null;
            String field = matcher.group(2);
            String condition = matcher.group(3);
            String value = matcher.group(4).replaceAll("\"", "");
            String operator = matcher.group(5);

            Filter newFilter = createFilter(field, condition, value);
            if (isNegated) {
                newFilter = new NotFilter(newFilter);
            }

            if (currentFilter == null) {
                currentFilter = newFilter;
            } else if ("AND".equalsIgnoreCase(lastOperator)) {
                currentFilter = new AndFilter(currentFilter, newFilter);
            } else if ("OR".equalsIgnoreCase(lastOperator)) {
                currentFilter = new OrFilter(currentFilter, newFilter);
            }

            lastOperator = operator;
        }

        if (currentFilter != null) {
            filters.add(currentFilter);
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

    @Override
    public boolean matches(JsonNode node) {
        for (Filter filter : filters) {
            if (!filter.matches(node)) {
                return false;
            }
        }
        return true;
    }

    public boolean validateObject(String serializedJsonString) {
        try {
            JsonNode node = mapper.readTree(serializedJsonString);
            return matches(node);
        } catch (Exception e) {
            return false;
        }
    }
}
