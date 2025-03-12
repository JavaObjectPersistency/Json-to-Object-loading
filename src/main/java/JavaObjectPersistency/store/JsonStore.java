package JavaObjectPersistency.store;

import JavaObjectPersistency.annotations.FieldAlias;
import JavaObjectPersistency.annotations.Id;
import JavaObjectPersistency.annotations.Persistent;
import JavaObjectPersistency.annotations.Transient;
import JavaObjectPersistency.query.Query;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.*;


public class JsonStore {
    private final ObjectMapper mapper = new ObjectMapper();

    private String getFileName(Class<?> type) {
        return type.getSimpleName() + ".json";
    }

    public void save(Object obj) throws Exception {
        Field idField = findIdField(obj.getClass());
        idField.setAccessible(true);
        Object id = idField.get(obj);

        String fileName = getFileName(obj.getClass());
        File file = new File(fileName);

        Map<String, Object> storage = new HashMap<>();
        if (file.exists()) {
            storage = mapper.readValue(file, Map.class);
        }

        JsonNode jsonNode = serializeObject(obj);
        storage.put(String.valueOf(id), jsonNode); // Преобразуем любой тип в строку

        try (FileOutputStream fos = new FileOutputStream(file)) {
            mapper.writeValue(fos, storage);
        }
    }

    private Field findIdField(Class<?> type) {
        for (Field field : type.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                return field;
            }
        }
        throw new IllegalArgumentException("No @Id field found in class " + type.getName());
    }

    public <T, ID> List<T> loadById(Class<T> type, ID id) throws Exception {
        if (!type.isAnnotationPresent(Persistent.class)) {
            throw new IllegalArgumentException("Not a @Persistent class");
        }

        String fileName = getFileName(type);
        File file = new File(fileName);
        if (!file.exists()) return Collections.emptyList();

        Map<String, Object> storage = mapper.readValue(file, Map.class);
        JsonNode jsonNode = mapper.valueToTree(storage.get(String.valueOf(id)));
        if (jsonNode == null) return Collections.emptyList();

        T object = deserializeObject(type, jsonNode);
        return Collections.singletonList(object);
    }

    private JsonNode serializeObject(Object obj) throws Exception {
        Class<?> type = obj.getClass();
        Map<String, Object> map = new HashMap<>();

        for (Field field : type.getDeclaredFields()) {
            if (field.isAnnotationPresent(Transient.class)) continue;
            field.setAccessible(true);

            String fieldName = field.isAnnotationPresent(FieldAlias.class) ?
                    field.getAnnotation(FieldAlias.class).value() : field.getName();

            Object value = field.get(obj);

            if (value != null && value.getClass().isAnnotationPresent(Persistent.class)) {
                Field idField = findIdField(value.getClass());
                idField.setAccessible(true);
                map.put(fieldName, idField.get(value));
            } else {
                map.put(fieldName, value);
            }
        }

        return mapper.valueToTree(map);
    }


    private <T> T deserializeObject(Class<T> type, JsonNode jsonNode) throws Exception {
        T instance = type.getDeclaredConstructor().newInstance();

        for (Field field : type.getDeclaredFields()) {
            if (field.isAnnotationPresent(Transient.class)) continue;
            field.setAccessible(true);
            String fieldName = field.isAnnotationPresent(FieldAlias.class) ?
                    field.getAnnotation(FieldAlias.class).value() : field.getName();
            JsonNode valueNode = jsonNode.get(fieldName);
            if (valueNode != null) {
                if (field.getType().isAnnotationPresent(Persistent.class)) {
                    List<?> refs = loadById(field.getType(), valueNode.asInt());
                    if (!refs.isEmpty()) field.set(instance, refs.getFirst());
                } else {
                    field.set(instance, mapper.treeToValue(valueNode, field.getType()));
                }
            }
        }
        return instance;
    }


    public <T> List<T> loadStream(Class<T> type, Query query) throws Exception {
        if (!type.isAnnotationPresent(Persistent.class)) {
            throw new IllegalArgumentException("Not a @Persistent class");
        }

        String fileName = getFileName(type);
        File file = new File(fileName);
        if (!file.exists()) return Collections.emptyList();

        Map<String, Object> storage = mapper.readValue(file, Map.class);
        List<T> result = new ArrayList<>();

        for (Map.Entry<String, Object> entry : storage.entrySet()) {
            JsonNode jsonNode = mapper.valueToTree(entry.getValue());

            // Применяем фильтр
            if (query.matches(jsonNode)) {
                T object = deserializeObject(type, jsonNode);
                result.add(object);
            }
        }

        return result;
    }

}
