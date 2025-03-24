package JavaObjectPersistency;

import JavaObjectPersistency.classes.Person;
import JavaObjectPersistency.query.Query;
import JavaObjectPersistency.store.JsonStore;

import java.util.ArrayList;
import java.util.List;


public class Main {
    public static void main(String[] args) throws Exception {

        try {
            JsonStore store = new JsonStore();
            store.clearStorage(Person.class);

            // Create and save a parent
            Person john = new Person("John Doe", 35);
            store.save(john);
            System.out.println("Saved John with auto-generated UUID: " + john.getId());

            // Create and save a child
            Person jane = new Person("Jane Doe", 10);
            //store.save(jane);
            System.out.println("Saved Jane with auto-generated UUID: " + jane.getId());

            // Create a family relationship
            List<Person> johnsFamily = new ArrayList<>();
            johnsFamily.add(jane);
            //johnsFamily.add(john);
            john.setFamily(johnsFamily);
            store.save(john);

            System.out.println("Updated John with family reference to Jane");
//            List<Person> janesFamily = new ArrayList<>();
//            janesFamily.add(john);
//            jane.setFamily(janesFamily);
//            store.save(jane);
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
            Query adultQuery = new Query("age.greaterThan(18)");
            List<Person> adults = store.loadStream(Person.class, adultQuery);
            System.out.println("Found " + adults.size() + " adults:");
            for (Person adult : adults) {
                System.out.println(" - " + adult);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}