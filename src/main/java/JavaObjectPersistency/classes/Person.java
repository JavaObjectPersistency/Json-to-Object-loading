package JavaObjectPersistency.classes;

import JavaObjectPersistency.annotations.FieldAlias;
import JavaObjectPersistency.annotations.Id;
import JavaObjectPersistency.annotations.Persistent;
import JavaObjectPersistency.annotations.Transient;


@Persistent
public class Person {
    @Id
    private int id;
    @FieldAlias("fullName")
    private String name;
    @Transient
    private String temporaryData;

    public Person() {}
    public Person(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTemporaryData() { return temporaryData; }
    public void setTemporaryData(String temporaryData) { this.temporaryData = temporaryData; }

    @Override
    public String toString() {
        return "Person{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", temporaryData='" + temporaryData + '\'' +
                '}';
    }
}
