package JavaObjectPersistency.classes;

import JavaObjectPersistency.store.IdGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class IntIdGenerator implements IdGenerator {
    private static final ObjectMapper mapper = new ObjectMapper();

    private static String getFileName(Class<?> type) {
        return type.getSimpleName() + ".json";
    }

    public Object generateId(Object obj) throws IOException {
        String fileName = getFileName(obj.getClass());
        File file = new File(fileName);

        Map<String, Object> storage = file.exists()
                ? mapper.readValue(file, Map.class)
                : new HashMap<>();

        Integer uid = storage.keySet().size() + 1;
        return uid;
    }
}
