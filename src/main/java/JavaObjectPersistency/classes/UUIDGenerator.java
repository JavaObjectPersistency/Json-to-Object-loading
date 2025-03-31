package JavaObjectPersistency.classes;

import JavaObjectPersistency.store.IdGenerator;

import java.io.IOException;
import java.util.UUID;

public class UUIDGenerator implements IdGenerator {
    public Object generateId(Object obj) {
        return UUID.randomUUID();
    }
}
