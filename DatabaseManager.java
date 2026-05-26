import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all direct JDBC communication with railway_db.
 * Includes the CallableStatement for the BookTicket Stored Procedure.
 */
public class DatabaseManager {
    // CHANGED TO railway_db
    private static final String DB_URL = "jdbc:mysql://localhost:3306/railway_db"; 
    private static final String DB_USER = "root";
    private static final String DB_PASS = "Jatoi-05"; 
    private static boolean driverLoaded = false;

    static { loadDriver(); }
    private static void loadDriver() {
        if (!driverLoaded) {
            try { Class.forName("com.mysql.cj.jdbc.Driver"); driverLoaded = true; } 
            catch (ClassNotFoundException e) { System.err.println("FATAL: MySQL JDBC Driver not found!"); }
        }
    }
    public static boolean isDriverAvailable() { return driverLoaded; }
    public static Connection getConnection() throws SQLException {
        if (!driverLoaded) throw new SQLException("Driver not loaded.");
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }
    public static boolean testConnection() { try (Connection c = getConnection()) { return c.isValid(5); } catch (Exception e) { return false; } }

    // --- PASSENGER QUERIES ---
    public static List<Passenger> loadPassengers() {
        List<Passenger> list = new ArrayList<>();
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM Passenger ORDER BY passenger_id")) {
            while (rs.next()) list.add(Passenger.fromResultSet(rs));
        } catch (SQLException e) { System.err.println("Error loading passengers: " + e.getMessage()); }
        return list;
    }

    public static Passenger getPassengerById(int id) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM Passenger WHERE passenger_id = ?")) {
            ps.setInt(1, id); try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return Passenger.fromResultSet(rs); }
        } catch (SQLException e) { System.err.println("Error getting passenger: " + e.getMessage()); }
        return null;
    }

    public static int insertPassenger(String name, String email, String phone, String cnic, int age) {
        return insertAndGetId("INSERT INTO Passenger (full_name, email, phone, cnic, age) VALUES (?, ?, ?, ?, ?)", name, email, phone, cnic, age);
    }

    // --- SCHEDULE / VIEW QUERIES ---
    public static List<String> loadCities() {
        List<String> cities = new ArrayList<>();
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT DISTINCT city FROM Station ORDER BY city")) {
            while (rs.next()) cities.add(rs.getString("city"));
        } catch (SQLException e) { System.err.println("Error loading cities: " + e.getMessage()); }
        return cities;
    }

    public static List<AvailableSchedule> searchSchedules(String fromCity, String toCity) {
        List<AvailableSchedule> list = new ArrayList<>();
        String sql = "SELECT * FROM available_schedules WHERE from_city = ? AND to_city = ? ORDER BY departure_datetime";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fromCity); ps.setString(2, toCity);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(new AvailableSchedule(rs)); }
        } catch (SQLException e) { System.err.println("Error searching schedules: " + e.getMessage()); }
        return list;
    }

    // --- TICKET QUERIES ---
    public static List<Ticket> getTicketsByPassenger(int passengerId) {
        List<Ticket> list = new ArrayList<>();
        String sql = "SELECT tk.ticket_id, tk.passenger_id, p.full_name, tk.schedule_id, " +
                     "avs.train_name, avs.from_station, avs.to_station, tk.seat_number, tk.fare, " +
                     "tk.booking_date, tk.status " +
                     "FROM Ticket tk " +
                     "JOIN Passenger p ON tk.passenger_id = p.passenger_id " +
                     "LEFT JOIN available_schedules avs ON tk.schedule_id = avs.schedule_id " +
                     "WHERE tk.passenger_id = ? ORDER BY tk.booking_date DESC";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, passengerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new Ticket(
                    rs.getInt("ticket_id"), rs.getInt("passenger_id"), rs.getString("full_name"),
                    rs.getInt("schedule_id"), rs.getString("train_name"), rs.getString("from_station"),
                    rs.getString("to_station"), rs.getString("seat_number"), rs.getDouble("fare"),
                    rs.getString("booking_date"), rs.getString("status")
                ));
            }
        } catch (SQLException e) { System.err.println("Error loading tickets: " + e.getMessage()); }
        return list;
    }

    // --- STORED PROCEDURE CALL ---
    /**
     * Calls the BookTicket stored procedure using CallableStatement.
     * @return "SUCCESS" or "NO_SEATS" or "ERROR"
     */
    public static String callBookTicketProcedure(int passengerId, int scheduleId, String seatNo, double fare) {
        String result = "ERROR";
        String sql = "{CALL BookTicket(?, ?, ?, ?, ?)}";
        try (Connection conn = getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {
            
            cs.setInt(1, passengerId);
            cs.setInt(2, scheduleId);
            cs.setString(3, seatNo);
            cs.setDouble(4, fare);
            cs.registerOutParameter(5, Types.VARCHAR); // Register the OUT parameter
            
            cs.execute(); // Execute the procedure
            result = cs.getString(5); // Get the result
            
        } catch (SQLException e) {
            System.err.println("SP Error: " + e.getMessage());
            if (e.getMessage().contains("Foreign key constraint fails")) return "INVALID_ID";
        }
        return result;
    }

    // --- UTILITY ---
    private static void setParameters(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) { if (params[i] == null) ps.setNull(i + 1, Types.NULL); else ps.setObject(i + 1, params[i]); }
    }
    public static int insertAndGetId(String sql, Object... params) {
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setParameters(ps, params); ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { System.err.println("DB Insert Error: " + e.getMessage()); }
        return -1;
    }
}