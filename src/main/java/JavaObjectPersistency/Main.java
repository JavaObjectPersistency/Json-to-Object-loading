package JavaObjectPersistency;

import JavaObjectPersistency.classes.Person;
import JavaObjectPersistency.query.Query;
import JavaObjectPersistency.store.JsonStore;

import java.util.List;


public class Main {
    public static void main(String[] args) throws Exception {

        JsonStore store = new JsonStore();
        store.save(new Person(1, "John Doe"));
        store.save(new Person(2, "Jane Smoth"));
        store.save(new Person(3, "Tom Brown"));

        Query query = new Query("(fullName.contains(\"Jo\"))");
        List<Person> filteredPersons = store.loadStream(Person.class, query);

        System.out.println(filteredPersons);

        Query complexQuery = new Query("(fullName.contains(\"o\")) AND (id.lessThan(3))");
        List<Person> result = store.loadStream(Person.class, complexQuery);

        System.out.println(result);
    }
}