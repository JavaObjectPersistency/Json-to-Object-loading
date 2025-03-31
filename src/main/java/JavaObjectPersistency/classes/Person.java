package JavaObjectPersistency.classes;

import JavaObjectPersistency.annotations.Id;
import JavaObjectPersistency.annotations.Persistent;
import JavaObjectPersistency.annotations.Transient;
import JavaObjectPersistency.annotations.FieldAlias;

import java.util.List;
import java.util.UUID;

@Persistent
public class Person {
    @Id
    private Object id; // Will be automatically generated if null

    @FieldAlias("fullName")
    private String name;

    private int age;

    @Transient
    private String temporaryData; // This field won't be serialized

    private List<Person> family;

    // Default constructor needed for deserialization
    public Person() {
    }

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    // Getters and setters

    public Object getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getTemporaryData() {
        return temporaryData;
    }

    public void setTemporaryData(String temporaryData) {
        this.temporaryData = temporaryData;
    }

    public List<Person> getFamily() {
        return family;
    }

    public void setFamily(List<Person> family) {
        this.family = family;
    }

    @Override
    public String toString() {
        return "Person{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", age=" + age +
                ", family size=" + (family != null ? family.size() : 0) +
                '}';
    }
}
