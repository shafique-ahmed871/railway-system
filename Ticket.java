import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Model class representing a booked Ticket.
 */
public class Ticket {
    private int ticketId;
    private int passengerId;
    private String passengerName;
    private int scheduleId;
    private String trainName;
    private String fromStation;
    private String toStation;
    private String seatNumber;
    private double fare;
    private String bookingDate;
    private String status;

    public Ticket(int ticketId, int passengerId, String passengerName, int scheduleId, 
                  String trainName, String fromStation, String toStation, 
                  String seatNumber, double fare, String bookingDate, String status) {
        this.ticketId = ticketId;
        this.passengerId = passengerId;
        this.passengerName = passengerName;
        this.scheduleId = scheduleId;
        this.trainName = trainName;
        this.fromStation = fromStation;
        this.toStation = toStation;
        this.seatNumber = seatNumber;
        this.fare = fare;
        this.bookingDate = bookingDate;
        this.status = status;
    }

    public int getTicketId() { return ticketId; }
    public String getPassengerName() { return passengerName; }
    public int getScheduleId() { return scheduleId; }
    public String getTrainName() { return trainName; }
    public String getFromStation() { return fromStation; }
    public String getToStation() { return toStation; }
    public String getSeatNumber() { return seatNumber; }
    public double getFare() { return fare; }
    public String getBookingDate() { return bookingDate; }
    public String getStatus() { return status; }
}