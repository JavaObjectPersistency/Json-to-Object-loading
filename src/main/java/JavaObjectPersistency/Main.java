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
//            uuidStore.save(john);

            System.out.println("Updated John with family reference to Jane");
            System.out.println("Saved Jane with auto-generated UUID: " + jane.getId());

            List<Person> janesFamily = new ArrayList<>();
            janesFamily.add(john);
            jane.setFamily(janesFamily);
//            uuidStore.save(jane);
            uuidStore.save(john);

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
            Person haley = new Person("Haley Sanes", 29);
            uuidStore.save(haley);

            Person karen = new Person("Karen Randol", 48);
            uuidStore.save(karen);

            Person andre = new Person("Andre Larcade", 81);
            uuidStore.save(andre);

            Person calvin = new Person("Calvin Wisseman", 7);
            uuidStore.save(calvin);

            // Load all adult persons (age > 18)
            Query adultQuery = new Query("(age.greaterThan(18)))");
            List<Person> adults = uuidStore.loadStream(Person.class, adultQuery);
            System.out.println("Found " + adults.size() + " adults:");
            for (Person adult : adults) {
                System.out.println(" - " + adult);
                if(adult.getName().equals("John Doe")){
                    adult.setName("New John Doe"); //same object in java
                    uuidStore.save(adult);
                }
            }

            // Change John's age
            john.setAge(36);
            uuidStore.save(john);

            //Query complexQuery = new Query("((age.greaterThan(18)) AND (age.lessThan(50)) OR (fullName.contains('Jane'))");
            Query complexQuery = new Query("(age.greaterThan(18)) OR (age.lessThan(9)) ");
            List<Person> complexPeople = uuidStore.loadStream(Person.class, complexQuery);
            System.out.println("Found " + complexPeople.size() + " adults:");
            for (Person complexHuman : complexPeople) {
                System.out.println(" - " + complexHuman);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}