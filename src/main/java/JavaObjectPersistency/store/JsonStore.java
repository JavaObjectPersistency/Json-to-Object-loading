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
    private final Map<Class<?>, Map<Object, Object>> objectCache = new HashMap<>();
    private final ThreadLocal<Set<Object>> loadingObjects = ThreadLocal.withInitial(HashSet::new);
    private IdGenType idGenStrategy; // Store the ID generation strategy


    // Constructor that accepts the ID generation strategy
    public JsonStore(IdGenType idGenStrategy) {
        this.idGenStrategy = idGenStrategy;
    }

    // Default constructor with a default strategy
    public JsonStore() {
        this.idGenStrategy = IdGenType.UUID; // Default to UUID
    }

    // Method to change the strategy at runtime if needed
    public void setIdGenerationStrategy(IdGenType idGenStrategy) {
        this.idGenStrategy = idGenStrategy;
    }

    private String getFileName(Class<?> type) {
        return type.getSimpleName() + ".json";
    }

    // Simplified save method that uses the stored strategy
    public void save(Object obj) throws Exception {
        // Delegate to the existing implementation
        save(obj, this.idGenStrategy);
    }

    public void save(Object obj, IdGenType mode) throws Exception {
        // Проверяем, есть ли объект с таким же ID уже в кеше
        Field idField = findIdField(obj.getClass());
        idField.setAccessible(true);
        Object id = idField.get(obj);

        // Если ID не установлен, генерируем новый
        if (id == null) {
            Object uuid = mode.getGenerator().generateId(obj);
            idField.set(obj, uuid);
            id = uuid;
        }

        // Проверяем, есть ли этот объект уже в кеше
        Object cachedObj = getFromCache(obj.getClass(), id);
        if (cachedObj != null && cachedObj != obj) {
            // Если объект с таким ID уже есть в кеше, но это другой экземпляр,
            // копируем все поля из нового объекта в кешированный
            copyFields(obj, cachedObj);
            // И используем кешированный объект для сохранения
            obj = cachedObj;
        }

        saveRecursive(obj, new HashSet<>(), mode);
    }

    private void copyFields(Object source, Object target) throws Exception {
        Class<?> type = source.getClass();
        for (Field field : type.getDeclaredFields()) {
            if (field.isAnnotationPresent(Transient.class)) continue;
            if (field.isAnnotationPresent(Id.class)) continue; // ID не копируем

            field.setAccessible(true);
            field.set(target, field.get(source));
        }
    }

    private void saveRecursive(Object obj, Set<Object> processed, IdGenType mode) throws Exception {
        if (obj == null || processed.contains(obj)) {
            return;
        }
        processed.add(obj);

        String fileName = getFileName(obj.getClass());
        File file = new File(fileName);

        Map<String, Object> storage = file.exists()
                ? mapper.readValue(file, Map.class)
                : new HashMap<>();

        Field idField = findIdField(obj.getClass());
        idField.setAccessible(true);
        Object id = idField.get(obj);

        if (id == null) {
            Object uuid = mode.getGenerator().generateId(obj);
            idField.set(obj, uuid);
            id = uuid;
        }

        // Добавляем объект в кеш до обработки зависимостей
        addToCache(obj);

        for (Field field : obj.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Transient.class)) continue;
            field.setAccessible(true);

            Object value = field.get(obj);
            if (value != null) {
                if (value.getClass().isAnnotationPresent(Persistent.class)) {
                    saveRecursive(value, processed, mode);
                } else if (value instanceof Collection<?> collection) {
                    for (Object element : collection) {
                        if (element != null && element.getClass().isAnnotationPresent(Persistent.class)) {
                            saveRecursive(element, processed, mode);
                        }
                    }
                }
            }
        }

        JsonNode jsonNode = serializeObject(obj);
        storage.put(id.toString(), jsonNode);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(fos, storage);
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

    public <T> List<T> loadById(Class<T> type, Object id) throws Exception {
        if (!type.isAnnotationPresent(Persistent.class)) {
            throw new IllegalArgumentException("Not a @Persistent class");
        }

        Object cachedObject = getFromCache(type, id);
        if (cachedObject != null) {
            return Collections.singletonList((T) cachedObject);
        }

        if (loadingObjects.get().contains(id)) {
            return Collections.emptyList();
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

        // Сначала установим ID, чтобы можно было добавить объект в кеш
        Field idField = findIdField(type);
        idField.setAccessible(true);
        JsonNode idNode = jsonNode.get(idField.getName());
        if (idNode != null) {
            Object idValue = mapper.treeToValue(idNode, idField.getType());
            idField.set(instance, idValue);

            // Проверяем, есть ли объект с таким ID уже в кеше
            Object cachedInstance = getFromCache(type, idValue);
            if (cachedInstance != null) {
                return (T) cachedInstance;
            }
        }

        // Добавляем в кеш до заполнения полей
        addToCache(instance);

        for (Field field : type.getDeclaredFields()) {
            if (field.isAnnotationPresent(Transient.class) || field.isAnnotationPresent(Id.class)) continue;
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
                mapper.writerWithDefaultPrettyPrinter().writeValue(fos, emptyStorage);
            }
            System.out.println("Storage cleared for " + type.getSimpleName());
        } else {
            System.out.println("No storage file found for " + type.getSimpleName());
        }

        // Очищаем кеш для этого типа
        objectCache.remove(type);
    }

    private void addToCache(Object obj) throws Exception {
        Field idField = findIdField(obj.getClass());
        idField.setAccessible(true);
        Object id = idField.get(obj);
        if (id != null) {
            objectCache.computeIfAbsent(obj.getClass(), k -> new HashMap<>()).put(id, obj);
        }
    }

    private <T> T getFromCache(Class<T> type, Object id) {
        if (id == null) return null;
        Map<Object, Object> typeCache = objectCache.get(type);
        if (typeCache != null) {
            return (T) typeCache.get(id);
        }
        return null;
    }

    public void clearCache() {
        objectCache.clear();
    }
}
