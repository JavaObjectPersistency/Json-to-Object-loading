package JavaObjectPersistency.store;

import java.io.IOException;

public interface IdGenerator {
    default Object generateId(Object obj) throws IOException {
        return null;
    }
}
