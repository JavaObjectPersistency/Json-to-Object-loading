package JavaObjectPersistency;

import JavaObjectPersistency.classes.Person;
import JavaObjectPersistency.classes.PersonIntId;
import JavaObjectPersistency.query.Query;
import JavaObjectPersistency.store.IdGenType;
import JavaObjectPersistency.store.JsonStore;

import java.util.ArrayList;
import java.util.List;


public class Main {
    public static void main(String[] args) {

        try {
            JsonStore store = new JsonStore();
            store.clearStorage(Person.class);

            // Create and save a parent
            Person john = new Person("John Doe", 35);
            store.save(john, IdGenType.UUID);
            System.out.println("Saved John with auto-generated UUID: " + john.getId());

            // Create and save a child
            Person jane = new Person("Jane Doe", 10);
            store.save(jane, IdGenType.UUID);
            System.out.println("Saved Jane with auto-generated UUID: " + jane.getId());

            // Create a family relationship
            List<Person> johnsFamily = new ArrayList<>();
            johnsFamily.add(jane);
            john.setFamily(johnsFamily);
            store.save(john, IdGenType.UUID);

            System.out.println("Updated John with family reference to Jane");
            System.out.println("Saved Jane with auto-generated UUID: " + jane.getId());

            List<Person> janesFamily = new ArrayList<>();
            janesFamily.add(john);
            jane.setFamily(janesFamily);
            store.save(jane, IdGenType.UUID);

            // Load John by ID
            List<Person> loadedJohns = store.loadById(Person.class, john.getId());
            if (!loadedJohns.isEmpty()) {
                Person loadedJohn = loadedJohns.getFirst();
                System.out.println("Loaded John: " + loadedJohn);

                // Access Jane through John's family
                if (loadedJohn.getFamily() != null && !loadedJohn.getFamily().isEmpty()) {
                    Person loadedJane = loadedJohn.getFamily().getFirst();
                    System.out.println("John's family member: " + loadedJane);
                }
            }

            // Load all adult persons (age > 18)
            Query adultQuery = new Query("(age.greaterThan(18)))");
            //Query adultQuery = new Query("(age.greaterThan(18)) OR (fullName.contains('Jane'))");
            List<Person> adults = store.loadStream(Person.class, adultQuery);
            System.out.println("Found " + adults.size() + " adults:");
            for (Person adult : adults) {
                System.out.println(" - " + adult);
                adult.setName("NewJohn");
                store.save(adult, IdGenType.UUID);
            }

            // Change John's age
            john.setAge(36);
            store.save(john, IdGenType.UUID);

            // Create and save a parent
            Person karl = new Person("Karl", 35);
            store.save(karl, IdGenType.INT);
            System.out.println("Saved Karl with auto-generated INT ID: " + karl.getId());

            // Create and save a child
            Person paul = new Person("Paul", 10);
            store.save(paul, IdGenType.INT);
            System.out.println("Saved Paul with auto-generated INT ID: " + paul.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}