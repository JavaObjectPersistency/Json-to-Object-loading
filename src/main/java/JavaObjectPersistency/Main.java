package JavaObjectPersistency;

import JavaObjectPersistency.classes.Person;
import JavaObjectPersistency.store.JsonStore;

import java.util.List;


public class Main {
    public static void main(String[] args) throws Exception {
        JsonStore store = new JsonStore();

        Person person = new Person("gfkgjfkg", "John Doe");
        person.setTemporaryData("temp");
        store.save(person);

        List<Person> loadedPerson = store.loadById(Person.class, 1);
        System.out.println(
                "Loaded Person: " + loadedPerson.getFirst().getName() + " with id " + loadedPerson.getFirst().getId()
        );
    }
}