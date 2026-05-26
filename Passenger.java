import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Model class representing a Passenger.
 */
public class Passenger {
    private int passengerId;
    private String fullName;
    private String email;
    private String phone;
    private String cnic;
    private int age;

    public Passenger(int passengerId, String fullName, String email, String phone, String cnic, int age) {
        this.passengerId = passengerId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.cnic = cnic;
        this.age = age;
    }

    public int getPassengerId() { return passengerId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getCnic() { return cnic; }
    public int getAge() { return age; }

    public static Passenger fromResultSet(ResultSet rs) throws SQLException {
        return new Passenger(
            rs.getInt("passenger_id"), rs.getString("full_name"), rs.getString("email"),
            rs.getString("phone"), rs.getString("cnic"), rs.getInt("age")
        );
    }
}