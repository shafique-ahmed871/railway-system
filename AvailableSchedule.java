import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;

/**
 * Model class mapping directly to the 'available_schedules' SQL View.
 */
public class AvailableSchedule {
    private int scheduleId;
    private String trainName;
    private String trainType;
    private String fromStation;
    private String toStation;
    private String fromCity;
    private String toCity;
    private String departureDatetime;
    private String arrivalDatetime;
    private String scheduleStatus;
    private double distanceKm;
    private int durationMins;
    private int seatsLeft;

    public AvailableSchedule(ResultSet rs) throws SQLException {
        this.scheduleId = rs.getInt("schedule_id");
        this.trainName = rs.getString("train_name");
        this.trainType = rs.getString("train_type");
        this.fromStation = rs.getString("from_station");
        this.toStation = rs.getString("to_station");
        this.fromCity = rs.getString("from_city");
        this.toCity = rs.getString("to_city");
        this.departureDatetime = rs.getString("departure_datetime");
        this.arrivalDatetime = rs.getString("arrival_datetime");
        this.scheduleStatus = rs.getString("schedule_status");
        BigDecimal dist = rs.getBigDecimal("distance_km");
        this.distanceKm = dist != null ? dist.doubleValue() : 0.0;
        this.durationMins = rs.getInt("duration_mins");
        this.seatsLeft = rs.getInt("seats_left");
    }

    public int getScheduleId() { return scheduleId; }
    public String getTrainName() { return trainName; }
    public String getTrainType() { return trainType; }
    public String getFromStation() { return fromStation; }
    public String getToStation() { return toStation; }
    public String getDepartureDatetime() { return departureDatetime; }
    public String getArrivalDatetime() { return arrivalDatetime; }
    public String getScheduleStatus() { return scheduleStatus; }
    public double getDistanceKm() { return distanceKm; }
    public int getDurationMins() { return durationMins; }
    public int getSeatsLeft() { return seatsLeft; }
}