import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class DepotGUI extends JFrame {
    // Update these color constants at the start of your class variables
    private final Color primaryColor = new Color(0, 75, 140);
    private final Color secondaryColor = new Color(0, 90, 160);
    private final Color accentColor = new Color(0, 105, 180);
    private final DepotSystem depotSystem;
    private final JTextArea mainDisplayArea;
    private final JLabel statusLabel;
    private final Timer statusUpdateTimer;

    public DepotGUI() {
        super("Depot Management System");
        this.depotSystem = new DepotSystem();
        this.depotSystem.initializeSystem();

        // Set up the main frame
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Create main components
        JPanel sidebarPanel = createSidebar();
        mainDisplayArea = createMainDisplayArea();
        statusLabel = createStatusLabel();

        // Layout setup
        add(sidebarPanel, BorderLayout.WEST);
        add(new JScrollPane(mainDisplayArea), BorderLayout.CENTER);
        add(createStatusPanel(), BorderLayout.SOUTH);

        // Initialize status update timer
        statusUpdateTimer = new Timer(5000, e -> updateStatus());
        statusUpdateTimer.start();

        // Add clear display timer
        Timer clearTimer = new Timer(30000, e -> clearMainDisplay());
        clearTimer.start();

        // Apply modern look and feel
        applyModernStyle();
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(200, getHeight()));
        sidebar.setBackground(primaryColor);
        sidebar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Add buttons
        String[] buttonLabels = {
            "Process Parcel", "View Queue", "View Inventory",
            "System Log", "Add Customer", "Add Parcel",
            "Remove Customer", "Remove Parcel", "View Processed", "Exit" 
        };

        for (String label : buttonLabels) {
            JButton button = createStyledButton(label);
            sidebar.add(button);
            sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        return sidebar;
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(180, 40));
        button.setBackground(secondaryColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setOpaque(true);  
        button.setBorderPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        button.setFont(new Font("Arial", Font.BOLD, 12));

        button.addActionListener(e -> handleButtonClick(text));
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(accentColor);
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(secondaryColor);
            }
        });

        return button;
    }

    private JTextArea createMainDisplayArea() {
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setMargin(new Insets(10, 10, 10, 10));
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        textArea.setBackground(new Color(245, 245, 245));
        return textArea;
    }

    private JLabel createStatusLabel() {
        JLabel label = new JLabel("System Status: Ready");
        label.setFont(new Font("Arial", Font.PLAIN, 12));
        label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        return label;
    }

    private JPanel createStatusPanel() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.setBackground(new Color(240, 240, 240));
        statusPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        return statusPanel;
    }

    private void handleButtonClick(String buttonText) {
        switch (buttonText) {
            case "Process Parcel":
                showProcessParcelDialog();
                break;
            case "View Queue":
                updateMainDisplay(() -> depotSystem.displayRecipientList());
                break;
            case "View Inventory":
                updateMainDisplay(() -> depotSystem.displayInventory());
                break;
            case "System Log":
                updateMainDisplay(() -> depotSystem.displayEventLog());
                break;
            case "Add Customer":
                showAddCustomerDialog();
                break;
            case "Add Parcel":
                showAddParcelDialog();
                break;
            case "Remove Customer":
                showRemoveCustomerDialog();
                break;
            case "Remove Parcel":
                showRemoveParcelDialog();
                break;
            case "View Processed":
                displayProcessedParcels();
                break;    
            case "Exit":
                System.exit(0);
                break;
        }
    }

    private void displayProcessedParcels() {
        mainDisplayArea.setText(""); // Clear previous content
        mainDisplayArea.append("=== Processed Parcels ===\n\n");
        
        try (BufferedReader br = new BufferedReader(new FileReader("processed.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                mainDisplayArea.append(line + "\n");
            }
        } catch (IOException e) {
            mainDisplayArea.append("Error loading processed parcels: " + e.getMessage() + "\n");
        }
    }

    private void showProcessParcelDialog() {
        JDialog dialog = createStyledDialog("Process Parcel");
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        JTextField parcelIdField = new JTextField(20);
        JButton processButton = createStyledButton("Process");
        
        processButton.addActionListener(e -> {
            String parcelId = parcelIdField.getText().trim();
            if (!parcelId.isEmpty()) {
                // Create a temporary reader for this operation
                depotSystem.inputReader = new Scanner(parcelId);
                depotSystem.processNextRecipient();
                dialog.dispose();
                updateStatus();
                mainDisplayArea.append("Processed parcel: " + parcelId + "\n");
            } else {
                showError("Please enter a Parcel ID");
            }
        });

        // Add components to panel
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Enter Parcel ID:"), gbc);
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(parcelIdField, gbc);
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(processButton, gbc);

        dialog.add(panel);
        dialog.pack();
        dialog.setVisible(true);
    }

    private void showAddCustomerDialog() {
        JDialog dialog = createStyledDialog("Add New Customer");
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        JTextField nameField = new JTextField(20);
        JTextField parcelIdField = new JTextField(20);
        JButton addButton = createStyledButton("Add Customer");

        addButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            String parcelId = parcelIdField.getText().trim();
            if (!name.isEmpty() && !parcelId.isEmpty()) {
                // Create a temporary reader with the input values
                String input = name + "\n" + parcelId;
                depotSystem.inputReader = new Scanner(input);
                depotSystem.registerNewRecipient();
                dialog.dispose();
                updateStatus();
                mainDisplayArea.append("Added customer: " + name + " with parcel: " + parcelId + "\n");
            } else {
                showError("Please fill all fields");
            }
        });

        // Add components to panel
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Customer Name:"), gbc);
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(nameField, gbc);
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Parcel ID:"), gbc);
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(parcelIdField, gbc);
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(addButton, gbc);

        dialog.add(panel);
        dialog.pack();
        dialog.setVisible(true);
    }

    private void showAddParcelDialog() {
        JDialog dialog = createStyledDialog("Add New Parcel");
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
    
        JTextField idField = new JTextField(20);
        JSpinner weightSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 1000.0, 0.1));
        JTextField dimensionsField = new JTextField(20);
        JButton addButton = createStyledButton("Add Parcel");
    
        addButton.addActionListener(e -> {
            String id = idField.getText().trim();
            double weight = (Double) weightSpinner.getValue();
            String dimensions = dimensionsField.getText().trim();
            
            if (!id.isEmpty() && !dimensions.isEmpty()) {
                // Create a temporary reader with the input values
                String input = id + "\n" + weight + "\n" + dimensions;
                depotSystem.inputReader = new Scanner(input);
                depotSystem.registerNewPackage();
                dialog.dispose();
                updateStatus();
                mainDisplayArea.append("Added parcel: " + id + "\n");
            } else {
                showError("Please fill all fields");
            }
        });
    
        // Add components to panel
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Parcel ID:"), gbc);
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(idField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Weight (kg):"), gbc);
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(weightSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("Dimensions (LxWxH):"), gbc);
        gbc.gridx = 0; gbc.gridy = 5;
        panel.add(dimensionsField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 6;
        gbc.insets = new Insets(15, 5, 5, 5);
        panel.add(addButton, gbc);
    
        dialog.add(panel);
        dialog.pack();
        dialog.setVisible(true);
    }

    private void showRemoveCustomerDialog() {
        JDialog dialog = createStyledDialog("Remove Customer");
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
    
        JTextField nameField = new JTextField(20);
        JTextField parcelIdField = new JTextField(20);
        JButton removeButton = createStyledButton("Remove Customer");
    
        removeButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            String parcelId = parcelIdField.getText().trim();
            
            if (!name.isEmpty() && !parcelId.isEmpty()) {
                // Create a temporary reader with the input values
                String input = name + "\n" + parcelId;
                depotSystem.inputReader = new Scanner(input);
                depotSystem.deregisterRecipient();
                dialog.dispose();
                updateStatus();
                mainDisplayArea.append("Removed customer: " + name + "\n");
            } else {
                showError("Please fill all fields");
            }
        });
    
        // Add components to panel
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Customer Name:"), gbc);
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(nameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Parcel ID:"), gbc);
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(parcelIdField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.insets = new Insets(15, 5, 5, 5);
        panel.add(removeButton, gbc);
    
        dialog.add(panel);
        dialog.pack();
        dialog.setVisible(true);
    }

    private void showRemoveParcelDialog() {
        JDialog dialog = createStyledDialog("Remove Parcel");
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
    
        JTextField parcelIdField = new JTextField(20);
        JButton removeButton = createStyledButton("Remove Parcel");
    
        removeButton.addActionListener(e -> {
            String parcelId = parcelIdField.getText().trim();
            
            if (!parcelId.isEmpty()) {
                // Create a temporary reader with the input value
                depotSystem.inputReader = new Scanner(parcelId);
                depotSystem.removePackage();
                dialog.dispose();
                updateStatus();
                mainDisplayArea.append("Removed parcel: " + parcelId + "\n");
            } else {
                showError("Please enter a Parcel ID");
            }
        });
    
        // Add components to panel
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Parcel ID:"), gbc);
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(parcelIdField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.insets = new Insets(15, 5, 5, 5);
        panel.add(removeButton, gbc);
    
        dialog.add(panel);
        dialog.pack();
        dialog.setVisible(true);
    }

    
    private void showError(String message) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "Error",
            JOptionPane.ERROR_MESSAGE
        );
    }

    private JDialog createStyledDialog(String title) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return dialog;
    }

    private void updateMainDisplay(Runnable action) {
        // Redirect System.out to capture output
        CustomOutputStream cos = new CustomOutputStream(mainDisplayArea);
        PrintStream ps = new PrintStream(cos);
        PrintStream old = System.out;
        System.setOut(ps);

        // Execute the action
        action.run();

        // Restore original System.out
        System.setOut(old);
    }

    private void updateStatus() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        statusLabel.setText("Last Updated: " + now.format(formatter));
    }

    private void applyModernStyle() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Custom OutputStream to redirect System.out to JTextArea
    private static class CustomOutputStream extends OutputStream {
        private final JTextArea textArea;
        private final StringBuilder sb = new StringBuilder();

        public CustomOutputStream(JTextArea textArea) {
            this.textArea = textArea;
        }

        @Override
        public void write(int b) {
            sb.append((char) b);
            if (b == '\n') {
                textArea.append(sb.toString());
                sb.setLength(0);
            }
        }
    }

    private void clearMainDisplay() {
        mainDisplayArea.setText("");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DepotGUI gui = new DepotGUI();
            gui.setVisible(true);
        });
    }
}