package JavaObjectPersistency.store;

import JavaObjectPersistency.annotations.Id;
import JavaObjectPersistency.classes.IntIdGenerator;
import JavaObjectPersistency.classes.UUIDGenerator;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;


public enum IdGenType {
    UUID(new UUIDGenerator()),
    INT(new IntIdGenerator());

    private static final Map<Object, IdGenType> lookup
            = new HashMap<Object, IdGenType>();

    static {
        for (IdGenType s : EnumSet.allOf(IdGenType.class))
            lookup.put(s.getGenerator(), s);
    }

    private IdGenerator generator;

    private IdGenType(IdGenerator cls) {
        this.generator = cls;
    }

    public IdGenerator getGenerator() {
        return generator;
    }

    public static IdGenType get(int code) {
        return lookup.get(code);
    }
}
