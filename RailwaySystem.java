import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;

/**
 * Main GUI Class for Railway Management System.
 * Uses JTabbedPane for Search, Passengers, Booking, and Tickets.
 */
public class RailwaySystem extends JFrame {
    private JTabbedPane tabbedPane;
    private JTable scheduleTable, passengerTable, ticketTable;
    private DefaultTableModel scheduleModel, passengerModel, ticketModel;
    private JLabel statusLabel;
    private JComboBox<String> fromCityCombo, toCityCombo;

    private static final Color PRIMARY = new Color(41, 128, 185);
    private static final Color SUCCESS = new Color(39, 174, 96);
    private static final Color DANGER = new Color(231, 76, 60);
    private static final Color WARNING = new Color(243, 156, 18);

    public RailwaySystem() {
        setTitle("Pakistan Railway Management System");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        createMenuBar();
        createMainPanel();
        createStatusBar();
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); SwingUtilities.updateComponentTreeUI(this); } catch (Exception e) {}
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File"); JMenuItem exitItem = new JMenuItem("Exit"); exitItem.addActionListener(e -> System.exit(0)); fileMenu.add(exitItem); menuBar.add(fileMenu);
        setJMenuBar(menuBar);
    }

    private void createMainPanel() {
        tabbedPane = new JTabbedPane(); tabbedPane.setFont(new Font("Arial", Font.BOLD, 12));
        tabbedPane.addTab("Search Trains", createSearchPanel());
        tabbedPane.addTab("Passengers", createPassengerPanel());
        tabbedPane.addTab("Book Ticket", createBookingPanel());
        tabbedPane.addTab("My Tickets", createTicketsPanel());
        add(tabbedPane, BorderLayout.CENTER);
    }

    private void createStatusBar() { statusLabel = new JLabel(" Ready"); statusLabel.setBorder(BorderFactory.createEtchedBorder()); add(statusLabel, BorderLayout.SOUTH); }

    // --- PANEL 1: SEARCH TRAINS ---
    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10)); panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Top Search Bar
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("From City:"));
        fromCityCombo = new JComboBox<>(); loadCitiesIntoCombo(fromCityCombo); searchPanel.add(fromCityCombo);
        searchPanel.add(new JLabel("To City:"));
        toCityCombo = new JComboBox<>(); loadCitiesIntoCombo(toCityCombo); searchPanel.add(toCityCombo);
        JButton searchBtn = createStyledButton("Search Trains", PRIMARY); searchPanel.add(searchBtn);
        panel.add(searchPanel, BorderLayout.NORTH);

        // Results Table
        String[] cols = {"Schedule ID", "Train Name", "Type", "From", "To", "Departure", "Arrival", "Duration(mins)", "Seats Left", "Status"};
        scheduleModel = new DefaultTableModel(cols, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
        scheduleTable = new JTable(scheduleModel); scheduleTable.setRowHeight(25);
        
        // Highlight low seats in red
        scheduleTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected && column == 8) { 
                    try {
                        int seats = Integer.parseInt(value.toString());
                        if (seats <= 10) { c.setForeground(DANGER); setFont(getFont().deriveFont(Font.BOLD)); }
                        else { c.setForeground(table.getForeground()); setFont(getFont().deriveFont(Font.PLAIN)); }
                    } catch(Exception e) {}
                } else if (!isSelected) { c.setForeground(table.getForeground()); setFont(getFont().deriveFont(Font.PLAIN)); }
                return c;
            }
        });
        panel.add(new JScrollPane(scheduleTable), BorderLayout.CENTER);

        searchBtn.addActionListener(e -> {
            String from = (String) fromCityCombo.getSelectedItem();
            String to = (String) toCityCombo.getSelectedItem();
            if (from == null || to == null || from.equals(to)) { JOptionPane.showMessageDialog(this, "Select two different cities!"); return; }
            scheduleModel.setRowCount(0);
            for (AvailableSchedule s : DatabaseManager.searchSchedules(from, to)) {
                scheduleModel.addRow(new Object[]{s.getScheduleId(), s.getTrainName(), s.getTrainType(), s.getFromStation(), s.getToStation(), s.getDepartureDatetime(), s.getArrivalDatetime(), s.getDurationMins(), s.getSeatsLeft(), s.getScheduleStatus()});
            }
            if (scheduleModel.getRowCount() == 0) setStatus("No trains found for this route.", WARNING);
            else setStatus(scheduleModel.getRowCount() + " trains found!", SUCCESS);
        });
        return panel;
    }

    private void loadCitiesIntoCombo(JComboBox<String> combo) {
        for (String city : DatabaseManager.loadCities()) combo.addItem(city);
    }

    // --- PANEL 2: PASSENGERS ---
    private JPanel createPassengerPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10)); panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JLabel("Registered Passengers"), BorderLayout.NORTH);
        
        String[] cols = {"ID", "Full Name", "Email", "Phone", "CNIC", "Age"};
        passengerModel = new DefaultTableModel(cols, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
        passengerTable = new JTable(passengerModel); passengerTable.setRowHeight(25);
        panel.add(new JScrollPane(passengerTable), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = createStyledButton("Add Passenger", SUCCESS); addBtn.addActionListener(e -> showAddPassengerDialog());
        JButton refreshBtn = createStyledButton("Refresh", Color.GRAY); refreshBtn.addActionListener(e -> refreshPassengers());
        btnPanel.add(addBtn); btnPanel.add(refreshBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);
        
        refreshPassengers(); // Load on startup
        return panel;
    }

    private void showAddPassengerDialog() {
        JDialog d = new JDialog(this, "Register Passenger", true); d.setSize(400, 350); d.setLocationRelativeTo(this); d.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints(); gbc.insets = new Insets(8,8,8,8); gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextField nameF = new JTextField(20), emailF = new JTextField(20), phoneF = new JTextField(20), cnicF = new JTextField(20), ageF = new JTextField(20);

        gbc.gridx=0;gbc.gridy=0;d.add(new JLabel("Full Name*:"),gbc);gbc.gridx=1;d.add(nameF,gbc);
        gbc.gridx=0;gbc.gridy=1;d.add(new JLabel("Email*:"),gbc);gbc.gridx=1;d.add(emailF,gbc);
        gbc.gridx=0;gbc.gridy=2;d.add(new JLabel("Phone:"),gbc);gbc.gridx=1;d.add(phoneF,gbc);
        gbc.gridx=0;gbc.gridy=3;d.add(new JLabel("CNIC:"),gbc);gbc.gridx=1;d.add(cnicF,gbc);
        gbc.gridx=0;gbc.gridy=4;d.add(new JLabel("Age*:"),gbc);gbc.gridx=1;d.add(ageF,gbc);
        gbc.gridx=0;gbc.gridy=5;gbc.gridwidth=2;gbc.fill=GridBagConstraints.CENTER;d.add(createStyledButton("Save", SUCCESS),gbc);

        ((JButton)d.getContentPane().getComponent(10)).addActionListener(e -> {
            try {
                if(nameF.getText().trim().isEmpty()||emailF.getText().trim().isEmpty()) {JOptionPane.showMessageDialog(d,"Name and Email required!");return;}
                int age = Integer.parseInt(ageF.getText().trim()); if(age<1||age>120) {JOptionPane.showMessageDialog(d,"Invalid age!");return;}
                int id = DatabaseManager.insertPassenger(nameF.getText().trim(), emailF.getText().trim(), phoneF.getText().trim(), cnicF.getText().trim(), age);
                if(id>0){JOptionPane.showMessageDialog(d,"Passenger Registered! ID: "+id);refreshPassengers();d.dispose();setStatus("Passenger added!",SUCCESS);}
            } catch (Exception ex) {JOptionPane.showMessageDialog(d,"Invalid input!");}
        });
        d.setVisible(true);
    }

    private void refreshPassengers() {
        passengerModel.setRowCount(0);
        for(Passenger p : DatabaseManager.loadPassengers()) {
            passengerModel.addRow(new Object[]{p.getPassengerId(), p.getFullName(), p.getEmail(), p.getPhone(), p.getCnic(), p.getAge()});
        }
    }

    // --- PANEL 3: BOOK TICKET ---
    private JPanel createBookingPanel() {
        JPanel panel = new JPanel(new GridBagLayout()); panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints(); gbc.insets = new Insets(10,10,10,10); gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField schedIdF = new JTextField(15), passIdF = new JTextField(15), seatF = new JTextField(15), fareF = new JTextField(15);

        gbc.gridx=0;gbc.gridy=0;panel.add(new JLabel("Schedule ID (from Search tab):"),gbc);gbc.gridx=1;panel.add(schedIdF,gbc);
        gbc.gridx=0;gbc.gridy=1;panel.add(new JLabel("Passenger ID:"),gbc);gbc.gridx=1;panel.add(passIdF,gbc);
        gbc.gridx=0;gbc.gridy=2;panel.add(new JLabel("Seat Number (e.g. A-05):"),gbc);gbc.gridx=1;panel.add(seatF,gbc);
        gbc.gridx=0;gbc.gridy=3;panel.add(new JLabel("Fare (Rs):"),gbc);gbc.gridx=1;panel.add(fareF,gbc);
        
        gbc.gridx=0;gbc.gridy=4;gbc.gridwidth=2;gbc.fill=GridBagConstraints.CENTER;
        JButton bookBtn = createStyledButton("Book Ticket (via Stored Procedure)", PRIMARY); panel.add(bookBtn, gbc);

        bookBtn.addActionListener(e -> {
            try {
                int sId = Integer.parseInt(schedIdF.getText().trim());
                int pId = Integer.parseInt(passIdF.getText().trim());
                String seat = seatF.getText().trim();
                double fare = Double.parseDouble(fareF.getText().trim());

                if(seat.isEmpty()) { JOptionPane.showMessageDialog(this, "Enter seat number!"); return; }
                if(fare <= 0) { JOptionPane.showMessageDialog(this, "Fare must be > 0!"); return; }

                // Verify passenger exists
                if(DatabaseManager.getPassengerById(pId) == null) {
                    JOptionPane.showMessageDialog(this, "Passenger ID " + pId + " does not exist!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                setStatus("Calling BookTicket Stored Procedure...", WARNING);
                // CALL THE STORED PROCEDURE
                String result = DatabaseManager.callBookTicketProcedure(pId, sId, seat, fare);

                if ("SUCCESS".equals(result)) {
                    JOptionPane.showMessageDialog(this, "Ticket Booked Successfully!\nSeat: " + seat, "Success", JOptionPane.INFORMATION_MESSAGE);
                    setStatus("Ticket booked via SP!", SUCCESS);
                    schedIdF.setText(""); passIdF.setText(""); seatF.setText(""); fareF.setText("");
                } else if ("NO_SEATS".equals(result)) {
                    JOptionPane.showMessageDialog(this, "Booking Failed: No seats left on this train!", "Full", JOptionPane.WARNING_MESSAGE);
                    setStatus("Booking failed: Train full.", DANGER);
                } else if ("INVALID_ID".equals(result)) {
                    JOptionPane.showMessageDialog(this, "Invalid Schedule ID!", "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Error: " + result, "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter valid numbers for IDs and Fare!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        return panel;
    }

    // --- PANEL 4: MY TICKETS ---
    private JPanel createTicketsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10)); panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Enter Passenger ID:"));
        JTextField idF = new JTextField(10); topPanel.add(idF);
        JButton searchBtn = createStyledButton("View Tickets", PRIMARY); topPanel.add(searchBtn);
        panel.add(topPanel, BorderLayout.NORTH);

        String[] cols = {"Ticket ID", "Train", "From", "To", "Seat", "Fare (Rs)", "Booked On", "Status"};
        ticketModel = new DefaultTableModel(cols, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
        ticketTable = new JTable(ticketModel); ticketTable.setRowHeight(25);
        panel.add(new JScrollPane(ticketTable), BorderLayout.CENTER);

        searchBtn.addActionListener(e -> {
            try {
                int pId = Integer.parseInt(idF.getText().trim());
                ticketModel.setRowCount(0);
                for(Ticket t : DatabaseManager.getTicketsByPassenger(pId)) {
                    ticketModel.addRow(new Object[]{t.getTicketId(), t.getTrainName(), t.getFromStation(), t.getToStation(), t.getSeatNumber(), String.format("%.2f", t.getFare()), t.getBookingDate(), t.getStatus()});
                }
                if(ticketModel.getRowCount()==0) setStatus("No tickets found for this passenger.", WARNING);
                else setStatus("Found " + ticketModel.getRowCount() + " tickets.", SUCCESS);
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Enter valid Passenger ID!"); }
        });
        return panel;
    }

    // --- HELPERS ---
    private JButton createStyledButton(String text, Color bgColor) {
        JButton b = new JButton(text); b.setBackground(bgColor); b.setForeground(Color.WHITE); b.setFocusPainted(false); b.setFont(new Font("Arial", Font.BOLD, 11)); b.setCursor(new Cursor(Cursor.HAND_CURSOR)); return b;
    }
    private void setStatus(String msg, Color color) { statusLabel.setText(" " + msg); statusLabel.setForeground(color); }

    // --- MAIN ---
    public static void main(String[] args) {
        if (!DatabaseManager.isDriverAvailable()) { JOptionPane.showMessageDialog(null, "MySQL JDBC Driver not found!\nAdd mysql-connector-j.jar to your folder.", "Fatal Error", JOptionPane.ERROR_MESSAGE); System.exit(1); }
        if (!DatabaseManager.testConnection()) { JOptionPane.showMessageDialog(null, "Failed to connect to railway_db!\nCheck credentials and ensure SQL file was run.", "Connection Error", JOptionPane.ERROR_MESSAGE); System.exit(1); }
        SwingUtilities.invokeLater(() -> { try { RailwaySystem app = new RailwaySystem(); app.setVisible(true); app.setStatus("Connected to railway_db successfully!", SUCCESS); } catch (Exception e) { JOptionPane.showMessageDialog(null, "Error: " + e.getMessage(), "Startup Error", JOptionPane.ERROR_MESSAGE); e.printStackTrace(); } });
    }
}