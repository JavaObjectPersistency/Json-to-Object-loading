package JavaObjectPersistency;

import JavaObjectPersistency.classes.Person;
import JavaObjectPersistency.classes.DifferentPerson;
import JavaObjectPersistency.query.Query;
import JavaObjectPersistency.store.IdGenType;
import JavaObjectPersistency.store.JsonStore;

import java.util.ArrayList;
import java.util.List;


public class Main {
    public static void main(String[] args) {

        try {
            // Create store with UUID strategy
            JsonStore uuidStore = new JsonStore();
            uuidStore.clearStorage(Person.class);

            // Create and save a parent
            Person john = new Person("John Doe", 35);
            uuidStore.save(john);
            System.out.println("Saved John with auto-generated UUID: " + john.getId());

            // Create and save a child
            Person jane = new Person("Jane Doe", 10);
            uuidStore.save(jane);
            System.out.println("Saved Jane with auto-generated UUID: " + jane.getId());

            // Create a family relationship
            List<Person> johnsFamily = new ArrayList<>();
            johnsFamily.add(jane);
            john.setFamily(johnsFamily);
            uuidStore.save(john);

            System.out.println("Updated John with family reference to Jane");
            System.out.println("Saved Jane with auto-generated UUID: " + jane.getId());

            List<Person> janesFamily = new ArrayList<>();
            janesFamily.add(john);
            jane.setFamily(janesFamily);
            uuidStore.save(jane);

            // Load John by ID
            List<Person> loadedJohns = uuidStore.loadById(Person.class, john.getId());
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
            List<Person> adults = uuidStore.loadStream(Person.class, adultQuery);
            System.out.println("Found " + adults.size() + " adults:");
            for (Person adult : adults) {
                System.out.println(" - " + adult);
                adult.setName("NewJohn");
                uuidStore.save(adult);
            }

            // Change John's age
            john.setAge(36);
            uuidStore.save(john);


            // Create store with INT strategy for other objects
            JsonStore intStore = new JsonStore(IdGenType.INT);
            intStore.clearStorage(DifferentPerson.class);
            // Create and save a parent
            DifferentPerson karl = new DifferentPerson("Karl", 35);
            intStore.save(karl);
            System.out.println("Saved Karl with auto-generated INT ID: " + karl.getId());

            // Create and save a child
            DifferentPerson paul = new DifferentPerson("Paul", 10);
            intStore.save(paul);
            System.out.println("Saved Paul with auto-generated INT ID: " + paul.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}