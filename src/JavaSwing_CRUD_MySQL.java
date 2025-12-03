import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

/*
 * Java Swing CRUD example with:
 * - Exception handling (custom + SQLException handling)
 * - Static GUI elements (labels, fields)
 * - Dynamic GUI (buttons, event handling)
 * - MySQL (XAMPP) connection and CRUD using JDBC
 *
 * Instructions:
 * 1. Make sure MySQL is running (XAMPP). Create a database named `java_crud_db` or change DB_URL.
 *    Example SQL: CREATE DATABASE java_crud_db;
 * 2. Add MySQL Connector/J to your classpath (mysql-connector-java-8.x.xx.jar).
 * 3. Update DB_USER and DB_PASS below if needed.
 * 4. Compile and run: javac JavaSwing_CRUD_MySQL.java && java JavaSwing_CRUD_MySQL
 */

public class JavaSwing_CRUD_MySQL extends JFrame {

    // === DB CONFIG - change as necessary ===
    private static final String DB_URL = "jdbc:mysql://localhost:3306/java_crud_db?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root"; // change if you set a password
    private static final String DB_PASS = "";     // change if you set a password

    // GUI components
    private JTextField tfId = new JTextField(5);
    private JTextField tfName = new JTextField(20);
    private JTextField tfAge = new JTextField(5);

    private JButton btnCreate = new JButton("Create");
    private JButton btnUpdate = new JButton("Update");
    private JButton btnDelete = new JButton("Delete");
    private JButton btnRefresh = new JButton("Refresh");

    private DefaultTableModel tableModel = new DefaultTableModel(new String[]{"ID", "Name", "Age"}, 0);
    private JTable table = new JTable(tableModel);

    private DBHelper db;

    public JavaSwing_CRUD_MySQL() {
        super("Java Swing CRUD - MySQL (XAMPP)");
        db = new DBHelper();

        initComponents();
        initEvents();

        // Ensure table exists and load data
        try {
            db.initDB();
            loadTable();
        } catch (DataAccessException ex) {
            showError(ex.getMessage());
        }

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 450);
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        // Static GUI layout (labels, textfields)
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; inputPanel.add(new JLabel("ID (leave empty to auto):"), gbc);
        gbc.gridx = 1; inputPanel.add(tfId, gbc);

        gbc.gridx = 0; gbc.gridy = 1; inputPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1; inputPanel.add(tfName, gbc);

        gbc.gridx = 0; gbc.gridy = 2; inputPanel.add(new JLabel("Age:"), gbc);
        gbc.gridx = 1; inputPanel.add(tfAge, gbc);

        JPanel buttons = new JPanel();
        buttons.add(btnCreate);
        buttons.add(btnUpdate);
        buttons.add(btnDelete);
        buttons.add(btnRefresh);

        JPanel top = new JPanel(new BorderLayout());
        top.add(inputPanel, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);

        JScrollPane tableScroll = new JScrollPane(table);

        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(tableScroll, BorderLayout.CENTER);
    }

    private void initEvents() {
        // Dynamic GUI: event handlers
        btnCreate.addActionListener(e -> onCreate());
        btnUpdate.addActionListener(e -> onUpdate());
        btnDelete.addActionListener(e -> onDelete());
        btnRefresh.addActionListener(e -> loadTable());

        // When a row is selected, populate fields (dynamic behavior)
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                int r = table.getSelectedRow();
                tfId.setText(tableModel.getValueAt(r, 0).toString());
                tfName.setText(tableModel.getValueAt(r, 1).toString());
                tfAge.setText(tableModel.getValueAt(r, 2).toString());
            }
        });

        // Simple input validation: only digits allowed in age and id
        tfAge.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent ke) {
                char c = ke.getKeyChar();
                if (!Character.isDigit(c) && c != '\b') ke.consume();
            }
        });
        tfId.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent ke) {
                char c = ke.getKeyChar();
                if (!Character.isDigit(c) && c != '\b') ke.consume();
            }
        });
    }

    // --- CRUD handlers ---
    private void onCreate() {
        String name = tfName.getText().trim();
        String ageStr = tfAge.getText().trim();
        if (name.isEmpty() || ageStr.isEmpty()) {
            showError("Name and Age are required to create a record.");
            return;
        }
        try {
            int age = Integer.parseInt(ageStr);
            db.create(name, age);
            clearInputs();
            loadTable();
            showInfo("Record created.");
        } catch (NumberFormatException ex) {
            showError("Age must be a valid integer.");
        } catch (DataAccessException ex) {
            showError(ex.getMessage());
        }
    }

    private void onUpdate() {
        String idStr = tfId.getText().trim();
        String name = tfName.getText().trim();
        String ageStr = tfAge.getText().trim();
        if (idStr.isEmpty()) {
            showError("ID is required to update a record. Select a row or enter ID.");
            return;
        }
        if (name.isEmpty() || ageStr.isEmpty()) {
            showError("Name and Age cannot be empty.");
            return;
        }
        try {
            int id = Integer.parseInt(idStr);
            int age = Integer.parseInt(ageStr);
            int updated = db.update(id, name, age);
            if (updated > 0) {
                loadTable();
                showInfo("Record updated.");
            } else {
                showError("No record found with ID=" + id);
            }
        } catch (NumberFormatException ex) {
            showError("ID and Age must be integers.");
        } catch (DataAccessException ex) {
            showError(ex.getMessage());
        }
    }

    private void onDelete() {
        String idStr = tfId.getText().trim();
        if (idStr.isEmpty()) {
            showError("ID is required to delete a record. Select a row or enter ID.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure to delete ID=" + idStr + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            int id = Integer.parseInt(idStr);
            int deleted = db.delete(id);
            if (deleted > 0) {
                loadTable();
                clearInputs();
                showInfo("Record deleted.");
            } else {
                showError("No record found with ID=" + id);
            }
        } catch (NumberFormatException ex) {
            showError("ID must be an integer.");
        } catch (DataAccessException ex) {
            showError(ex.getMessage());
        }
    }

    private void loadTable() {
        try {
            tableModel.setRowCount(0);
            try (ResultSet rs = db.readAll()) {
                while (rs.next()) {
                    tableModel.addRow(new Object[]{rs.getInt("id"), rs.getString("name"), rs.getInt("age")});
                }
            }
        } catch (DataAccessException | SQLException ex) {
            showError("Failed to load data: " + ex.getMessage());
        }
    }

    private void clearInputs() {
        tfId.setText("");
        tfName.setText("");
        tfAge.setText("");
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    // === Entry point ===
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new JavaSwing_CRUD_MySQL().setVisible(true));
    }

    // === Custom Exception to wrap DB errors ===
    static class DataAccessException extends Exception {
        public DataAccessException(String msg, Throwable cause) {
            super(msg, cause);
        }
        public DataAccessException(String msg) { super(msg); }
    }

    // === DB Helper inner class ===
    class DBHelper {

        // initDB: create table if not exists
        public void initDB() throws DataAccessException {
            String sql = "CREATE TABLE IF NOT EXISTS persons (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "age INT NOT NULL" +
                    ") ENGINE=InnoDB;";
            try (Connection conn = getConnection();
                 Statement st = conn.createStatement()) {
                st.execute(sql);
            } catch (SQLException ex) {
                throw new DataAccessException("Failed to initialize database.", ex);
            }
        }

        private Connection getConnection() throws SQLException {
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        }

        public void create(String name, int age) throws DataAccessException {
            String sql = "INSERT INTO persons (name, age) VALUES (?, ?)";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setInt(2, age);
                ps.executeUpdate();
            } catch (SQLException ex) {
                throw new DataAccessException("Create failed.", ex);
            }
        }

        public ResultSet readAll() throws DataAccessException, SQLException {
            Connection conn = getConnection();
            // caller must close ResultSet; we'll return an open ResultSet tied to this connection
            PreparedStatement ps = conn.prepareStatement("SELECT id, name, age FROM persons ORDER BY id");
            return ps.executeQuery();
        }

        public int update(int id, String name, int age) throws DataAccessException {
            String sql = "UPDATE persons SET name = ?, age = ? WHERE id = ?";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setInt(2, age);
                ps.setInt(3, id);
                return ps.executeUpdate();
            } catch (SQLException ex) {
                throw new DataAccessException("Update failed.", ex);
            }
        }

        public int delete(int id) throws DataAccessException {
            String sql = "DELETE FROM persons WHERE id = ?";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                return ps.executeUpdate();
            } catch (SQLException ex) {
                throw new DataAccessException("Delete failed.", ex);
            }
        }
    }
}
