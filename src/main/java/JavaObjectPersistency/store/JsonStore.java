package JavaObjectPersistency.store;

import JavaObjectPersistency.annotations.FieldAlias;
import JavaObjectPersistency.annotations.Id;
import JavaObjectPersistency.annotations.Persistent;
import JavaObjectPersistency.annotations.Transient;
import JavaObjectPersistency.query.Query;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class JsonStore {
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<Class<?>, Map<UUID, Object>> objectCache = new HashMap<>();
    private final ThreadLocal<Set<UUID>> loadingObjects = ThreadLocal.withInitial(HashSet::new);

    private String getFileName(Class<?> type) {
        return type.getSimpleName() + ".json";
    }

    public void save(Object obj) throws Exception {
        saveRecursive(obj, new HashSet<>());
        clearCache();
    }

    private void saveRecursive(Object obj, Set<Object> processed) throws Exception {
        if (obj == null || processed.contains(obj)) {
            return;
        }
        processed.add(obj);

        Field idField = findIdField(obj.getClass());
        idField.setAccessible(true);
        Object id = idField.get(obj);

        if (id == null && idField.getType().equals(UUID.class)) {
            UUID uuid = UUID.randomUUID();
            idField.set(obj, uuid);
            id = uuid;
        }

        for (Field field : obj.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Transient.class)) continue;
            field.setAccessible(true);

            Object value = field.get(obj);
            if (value != null) {
                if (value.getClass().isAnnotationPresent(Persistent.class)) {
                    saveRecursive(value, processed);
                } else if (value instanceof Collection<?> collection) {
                    for (Object element : collection) {
                        if (element != null && element.getClass().isAnnotationPresent(Persistent.class)) {
                            saveRecursive(element, processed);
                        }
                    }
                }
            }
        }

        String fileName = getFileName(obj.getClass());
        File file = new File(fileName);

        Map<String, Object> storage = file.exists()
                ? mapper.readValue(file, Map.class)
                : new HashMap<>();

        JsonNode jsonNode = serializeObject(obj);
        storage.put(id.toString(), jsonNode);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            mapper.writeValue(fos, storage);
        }

        addToCache(obj);
    }

    private Field findIdField(Class<?> type) {
        for (Field field : type.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                return field;
            }
        }
        throw new IllegalArgumentException("No @Id field found in class " + type.getName());
    }

    public <T> List<T> loadById(Class<T> type, UUID id) throws Exception {
        if (!type.isAnnotationPresent(Persistent.class)) {
            throw new IllegalArgumentException("Not a @Persistent class");
        }

        Object cachedObject = getFromCache(type, id);
        if (cachedObject != null) {
            return Collections.singletonList((T) cachedObject);
        }

        if (loadingObjects.get().contains(id)) {
            return Collections.emptyList(); // Возвращаем пустой список, если объект уже загружается
        }

        loadingObjects.get().add(id);

        try {
            String fileName = getFileName(type);
            File file = new File(fileName);
            if (!file.exists()) return Collections.emptyList();

            Map<String, Object> storage = mapper.readValue(file, Map.class);
            JsonNode jsonNode = mapper.valueToTree(storage.get(id.toString()));
            if (jsonNode == null) return Collections.emptyList();

            T object = deserializeObject(type, jsonNode);
            addToCache(object);
            return Collections.singletonList(object);
        } finally {
            loadingObjects.get().remove(id);
        }
    }

    private JsonNode serializeObject(Object obj) throws Exception {
        Class<?> type = obj.getClass();
        ObjectNode node = mapper.createObjectNode();

        for (Field field : type.getDeclaredFields()) {
            if (field.isAnnotationPresent(Transient.class)) continue;
            field.setAccessible(true);

            String fieldName = field.getAnnotation(FieldAlias.class) != null
                    ? field.getAnnotation(FieldAlias.class).value()
                    : field.getName();

            Object value = field.get(obj);

            if (value instanceof Collection<?> collection) {
                node.set(fieldName, serializeCollection(collection));
            } else {
                node.set(fieldName, serializeValue(value));
            }
        }
        return node;
    }

    private JsonNode serializeCollection(Collection<?> collection) throws Exception {
        return mapper.valueToTree(collection.stream()
                .map(this::serializeValue)
                .toList());
    }

    private JsonNode serializeValue(Object value) {
        if (value != null && value.getClass().isAnnotationPresent(Persistent.class)) {
            try {
                Field idField = findIdField(value.getClass());
                idField.setAccessible(true);
                Object id = idField.get(value);
                return mapper.valueToTree(id);
            } catch (Exception e) {
                throw new RuntimeException("Error serializing object reference", e);
            }
        }
        return mapper.valueToTree(value);
    }

    private <T> T deserializeObject(Class<T> type, JsonNode jsonNode) throws Exception {
        T instance = type.getDeclaredConstructor().newInstance();
        addToCache(instance);

        for (Field field : type.getDeclaredFields()) {
            if (field.isAnnotationPresent(Transient.class)) continue;
            field.setAccessible(true);

            String fieldName = field.getAnnotation(FieldAlias.class) != null
                    ? field.getAnnotation(FieldAlias.class).value()
                    : field.getName();

            JsonNode valueNode = jsonNode.get(fieldName);
            if (valueNode == null) continue;

            if (Collection.class.isAssignableFrom(field.getType())) {
                Collection<Object> collection = createCollection(field.getType());
                for (JsonNode elementNode : valueNode) {
                    Object element = parseElement(field, elementNode);
                    collection.add(element);
                }
                field.set(instance, collection);
            } else {
                field.set(instance, parseValue(field.getType(), valueNode));
            }
        }
        return instance;
    }

    private Object parseElement(Field field, JsonNode elementNode) throws Exception {
        Class<?> elementType = resolveCollectionElementType(field);
        return parseValue(elementType, elementNode);
    }

    private Object parseValue(Class<?> targetType, JsonNode node) throws Exception {
        if (targetType.isAnnotationPresent(Persistent.class)) {
            UUID refId = UUID.fromString(node.asText());
            Object cachedObject = getFromCache(targetType, refId);
            if (cachedObject != null) {
                return cachedObject;
            }
            List<?> refs = loadById(targetType, refId);
            return refs.isEmpty() ? null : refs.getFirst();
        }
        return mapper.treeToValue(node, targetType);
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

            if (query.matches(jsonNode)) {
                T object = deserializeObject(type, jsonNode);
                result.add(object);
            }
        }

        return result;
    }

    private Collection<Object> createCollection(Class<?> collectionType) {
        if (List.class.isAssignableFrom(collectionType)) {
            return new ArrayList<>();
        } else if (Set.class.isAssignableFrom(collectionType)) {
            return new HashSet<>();
        }
        throw new IllegalArgumentException("Unsupported collection type: " + collectionType);
    }

    private Class<?> resolveCollectionElementType(Field field) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType pt) {
            Type[] typeArgs = pt.getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> clazz) {
                return clazz;
            }
        }
        throw new IllegalArgumentException("Could not resolve element type for field: " + field.getName());
    }

    public void clearStorage(Class<?> type) throws IOException {
        String fileName = getFileName(type);
        File file = new File(fileName);

        if (file.exists()) {
            Map<String, Object> emptyStorage = new HashMap<>();
            try (FileOutputStream fos = new FileOutputStream(file)) {
                mapper.writeValue(fos, emptyStorage);
            }
            System.out.println("Storage cleared for " + type.getSimpleName());
        } else {
            System.out.println("No storage file found for " + type.getSimpleName());
        }
    }

    private void addToCache(Object obj) throws Exception {
        Field idField = findIdField(obj.getClass());
        idField.setAccessible(true);
        UUID id = (UUID) idField.get(obj);
        objectCache.computeIfAbsent(obj.getClass(), k -> new HashMap<>()).put(id, obj);
    }

    private <T> T getFromCache(Class<T> type, UUID id) {
        Map<UUID, Object> typeCache = objectCache.get(type);
        if (typeCache != null) {
            return (T) typeCache.get(id);
        }
        return null;
    }

    private void clearCache() {
        objectCache.clear();
    }
}
