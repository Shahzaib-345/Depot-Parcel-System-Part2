import java.io.*;
import java.util.Queue;
import java.util.Scanner;

public class DepotSystem {
    private PackageCollection packageCollection;
    private QueueOfCustomers recipientQueue;
    private EventLogger logger;
    private CollectionProcessor processor;
    private Scanner inputReader;

    public DepotSystem() {
        packageCollection = new PackageCollection();
        recipientQueue = new QueueOfCustomers();
        logger = EventLogger.getInstance();
        processor = new CollectionProcessor();
        inputReader = new Scanner(System.in);
    }

    public void initializeSystem() {
        initializePackageList();
        initializeRecipientQueue();
    }

    private void initializePackageList() {
        try (BufferedReader fileReader = new BufferedReader(new FileReader("Inventory.csv"))) {
            String record;
            boolean skipHeader = true;
            while ((record = fileReader.readLine()) != null) {
                if (skipHeader) {
                    skipHeader = false;
                    continue;
                }
                String[] fields = record.split(",");
                if (fields.length >= 4) {
                    String identifier = fields[0].trim();
                    float mass = Float.parseFloat(fields[1].trim());
                    String dimensions = fields[2].trim() + "x" + fields[3].trim() + "x" + fields[4].trim();
                    Parcel newPackage = new Parcel(identifier, mass, dimensions);
                    packageCollection.addPackage(newPackage);
                }
            }
        } catch (IOException ex) {
            System.err.println("Error loading inventory data: " + ex.getMessage());
            logger.logEvent("Failed to initialize package list: " + ex.getMessage());
        }
    }

    private void initializeRecipientQueue() {
        try (BufferedReader fileReader = new BufferedReader(new FileReader("Recipients.csv"))) {
            String record;
            boolean skipHeader = true;
            int sequence = 1;
            while ((record = fileReader.readLine()) != null) {
                if (skipHeader) {
                    skipHeader = false;
                    continue;
                }
                String[] fields = record.split(",");
                if (fields.length >= 2) {
                    String name = fields[0].trim();
                    String packageId = fields[1].trim();
                    Customer recipient = new Customer(name, packageId, sequence++);
                    recipientQueue.enqueueRecipient(recipient);
                }
            }
        } catch (IOException ex) {
            System.err.println("Error loading recipient data: " + ex.getMessage());
            logger.logEvent("Failed to initialize recipient queue: " + ex.getMessage());
        }
    }

    public void startSystem() {
        boolean systemActive = true;
        while (systemActive) {
            showSystemMenu();
            String userChoice = inputReader.nextLine();

            switch (userChoice) {
                case "1": processNextRecipient(); break;
                case "2": displayRecipientList(); break;
                case "3": displayInventory(); break;
                case "4": displayEventLog(); break;
                case "5": registerNewRecipient(); break;
                case "6": registerNewPackage(); break;
                case "7": deregisterRecipient(); break;
                case "8": removePackage(); break;
                case "9": systemActive = false; break;
                default: System.out.println("Invalid selection. Try again.");
            }
        }
        inputReader.close();
    }

    private void showSystemMenu() {
        System.out.println("\n=== Depot Management System ===");
        System.out.println("1. Process Next Recipient");
        System.out.println("2. View Recipients Queue");
        System.out.println("3. View Inventory");
        System.out.println("4. View System Log");
        System.out.println("5. Register New Recipient");
        System.out.println("6. Register New Package");
        System.out.println("7. Deregister Recipient");
        System.out.println("8. Remove Package");
        System.out.println("9. Exit System");
        System.out.print("Select option: ");
    }

    public void processNextRecipient() {
        System.out.print("Enter package identifier: ");
        String packageId = inputReader.nextLine();
    
        // Check if package exists
        Parcel currentPackage = packageCollection.getPackageByID(packageId);
        if (currentPackage == null) {
            System.out.println("Package not found");
            logger.logEvent("Failed to process package: " + packageId + " - not found");
            return;
        }
    
        // Check if customer exists with this package
        Queue<Customer> queue = recipientQueue.getQueueContents();
        Customer recipientToProcess = null;
    
        for (Customer recipient : queue) {
            if (recipient.getPackageID().equals(packageId)) {
                recipientToProcess = recipient;
                break;
            }
        }
    
        if (recipientToProcess == null) {
            System.out.println("No recipient found with this package ID");
            logger.logEvent("Failed to process package: " + packageId + " - no recipient found");
            return;
        }
    
        // Calculate the fee using the processor
        float processingCharge = processor.computeCollectionCharge(currentPackage);
    
        // Process the recipient
        processor.processCollection(recipientToProcess, currentPackage);
    
        // Add details to released.csv
        try (FileWriter fw = new FileWriter("released.csv", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
    
            out.println(recipientToProcess.getSurname() + "," + recipientToProcess.getPackageID() + "," +
                        currentPackage.getMassKg() + "," + currentPackage.getMeasurementSpec() + "," +
                        currentPackage.getDeliveryState() + ",£" + String.format("%.2f", processingCharge));
            System.out.println("Details added to released.csv");
        } catch (IOException e) {
            System.err.println("Error updating released file: " + e.getMessage());
            logger.logEvent("Error adding release details to released.csv: " + e.getMessage());
        }
    
        updateSystemRecords(recipientToProcess, packageId);
    
        // Remove recipient from the in-memory queue
        recipientQueue.dequeueRecipient();
    
        System.out.println("Processed recipient: " + recipientToProcess.getSurname() + 
                          " with package: " + packageId +
                          ". Fee: £" + String.format("%.2f", processingCharge));
    }

    private void updateSystemRecords(Customer recipient, String packageId) {
        // Update Recipients file
        try {
            File inputFile = new File("Recipients.csv");
            File tempFile = new File("Recipients_temp.csv");
    
            try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] data = line.split(",");
                    if (!(data.length >= 2 && 
                        data[0].trim().equals(recipient.getSurname()) && 
                        data[1].trim().equals(packageId))) {
                        writer.write(line + System.lineSeparator());
                    }
                }
            }
    
            inputFile.delete();
            tempFile.renameTo(inputFile);
            logger.logEvent("Updated recipient records for: " + recipient.getSurname());
        } catch (IOException ex) {
            logger.logEvent("Failed to update recipient records: " + ex.getMessage());
        }
    
        // Update Inventory file
        try {
            File inputFile = new File("Inventory.csv");
            File tempFile = new File("Inventory_temp.csv");
    
            try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] data = line.split(",");
                    if (!(data.length >= 1 && data[0].trim().equals(packageId))) {
                        writer.write(line + System.lineSeparator());
                    }
                }
            }
    
            inputFile.delete();
            tempFile.renameTo(inputFile);
            logger.logEvent("Updated inventory records for package: " + packageId);
        } catch (IOException ex) {
            logger.logEvent("Failed to update inventory records: " + ex.getMessage());
        }
    }

    private void updateFile(String filename, java.util.function.Predicate<String> skipCondition) {
        try {
            File originalFile = new File(filename);
            File tempFile = new File(filename + ".tmp");
            
            try (BufferedReader reader = new BufferedReader(new FileReader(originalFile));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!skipCondition.test(line)) {
                        writer.write(line + System.lineSeparator());
                    }
                }
            }
            
            originalFile.delete();
            tempFile.renameTo(originalFile);
            
        } catch (IOException ex) {
            logger.logEvent("Failed to update " + filename + ": " + ex.getMessage());
        }
    }

    private void displayRecipientList() {
        System.out.println("\nCurrent Recipients Queue:");
        recipientQueue.getQueueContents().forEach(System.out::println);
    }

    private void displayInventory() {
        System.out.println("\nCurrent Inventory Status:");
        packageCollection.getAllPackages().forEach(System.out::println);
    }

    private void displayEventLog() {
        System.out.println("\nSystem Event History:");
        System.out.println(logger.getEventHistory());
    }

    private void registerNewRecipient() {
        System.out.print("Enter recipient name: ");
        String name = inputReader.nextLine();
        System.out.print("Enter package ID: ");
        String packageId = inputReader.nextLine();
    
        // Check if package exists
        if (packageCollection.getPackageByID(packageId) == null) {
            System.out.println("Error: Package ID does not exist.");
            logger.logEvent("Failed to add recipient " + name + ": Package ID " + packageId + " not found");
            return;
        }
    
        int sequence = recipientQueue.getQueueContents().size() + 1;
        Customer newRecipient = new Customer(name, packageId, sequence);
        recipientQueue.enqueueRecipient(newRecipient);
    
        try (FileWriter fw = new FileWriter("Recipients.csv", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(name + "," + packageId);
            logger.logEvent("Added new recipient: " + name + " with package ID: " + packageId);
        } catch (IOException e) {
            System.err.println("Error updating recipient file: " + e.getMessage());
            logger.logEvent("Error adding recipient to file: " + e.getMessage());
        }
    }

    private void registerNewPackage() {
        System.out.print("Enter package ID: ");
        String id = inputReader.nextLine();
        System.out.print("Enter weight: ");
        float mass = Float.parseFloat(inputReader.nextLine());
        System.out.print("Enter dimensions (length width height): ");
        String dimensions = inputReader.nextLine();
    
        Parcel newPackage = new Parcel(id, mass, dimensions);
        packageCollection.addPackage(newPackage);
    
        try (FileWriter fw = new FileWriter("Inventory.csv", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            String[] dims = dimensions.split(" ");
            out.println(id + "," + mass + "," + String.join(",", dims));
            logger.logEvent("Added new package: " + id + " with mass: " + mass + " and dimensions: " + dimensions);
        } catch (IOException e) {
            System.err.println("Error updating inventory file: " + e.getMessage());
            logger.logEvent("Error adding package to file: " + e.getMessage());
        }
    }

    private void deregisterRecipient() {
        System.out.print("Enter recipient name to remove: ");
        String name = inputReader.nextLine();
        System.out.print("Enter package ID: ");
        String packageId = inputReader.nextLine();
    
        Parcel parcel = packageCollection.getPackageByID(packageId);
        if (parcel != null && !parcel.getDeliveryState().equals("Collected")) {
            System.out.println("Cannot remove recipient: associated package is still in depot");
            logger.logEvent("Failed to remove recipient " + name + ": Package " + packageId + " is still in depot");
            return;
        }
    
        updateFile("Recipients.csv", line -> {
            String[] data = line.split(",");
            return data.length >= 2 && data[0].trim().equals(name) && data[1].trim().equals(packageId);
        });
        
        logger.logEvent("Removed recipient: " + name + " with package ID: " + packageId);
    }
    

    private void removePackage() {
        System.out.print("Enter package ID to remove: ");
        String id = inputReader.nextLine();
    
        Parcel packageToRemove = packageCollection.getPackageByID(id);
        if (packageToRemove == null) {
            System.out.println("Package not found");
            logger.logEvent("Failed to remove package " + id + ": not found");
            return;
        }
    
        if (!packageToRemove.getDeliveryState().equals("Collected")) {
            System.out.println("Cannot remove package: still in depot");
            logger.logEvent("Failed to remove package " + id + ": still in depot");
            return;
        }
    
        updateFile("Inventory.csv", line -> {
            String[] data = line.split(",");
            return data.length >= 1 && data[0].trim().equals(id);
        });
        
        logger.logEvent("Removed package: " + id);
    }

    public static void main(String[] args) {
        DepotSystem system = new DepotSystem();
        system.initializeSystem();
        system.startSystem();
    }
}