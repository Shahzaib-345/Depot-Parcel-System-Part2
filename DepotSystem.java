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
                    float itemMass = Float.parseFloat(fields[1].trim());
                    String itemSize = fields[2].trim() + "x" + fields[3].trim() + "x" + fields[4].trim();
                    Parcel newItem = new Parcel(identifier, itemMass, itemSize);
                    packageCollection.addPackage(newItem);
                }
            }
        } catch (IOException ex) {
            System.err.println("Inventory loading failed: " + ex.getMessage());
        }
    }

    private void initializeRecipientQueue() {
        try (BufferedReader fileReader = new BufferedReader(new FileReader("Recipients.csv"))) {
            String record;
            boolean skipHeader = true;
            int tokenNumber = 1;
            while ((record = fileReader.readLine()) != null) {
                if (skipHeader) {
                    skipHeader = false;
                    continue;
                }
                String[] fields = record.split(",");
                if (fields.length >= 2) {
                    String recipientName = fields[0].trim();
                    String packageId = fields[1].trim();
                    Customer person = new Customer(recipientName, packageId, tokenNumber++);
                    recipientQueue.enqueueRecipient(person);
                }
            }
        } catch (IOException ex) {
            System.err.println("Recipients loading failed: " + ex.getMessage());
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
    
        Parcel currentPackage = packageCollection.getPackageByID(packageId);
        if (currentPackage == null) {
            System.out.println("Package not found in system");
            logger.logEvent("Processing failed: Package " + packageId + " not found");
            return;
        }
    
        Queue<Customer> activeQueue = recipientQueue.getQueueContents();
        Customer currentRecipient = null;
    
        for (Customer r : activeQueue) {
            if (r.getPackageID().equals(packageId)) {
                currentRecipient = r;
                break;
            }
        }
    
        if (currentRecipient == null) {
            System.out.println("No recipient found for this package");
            logger.logEvent("Processing failed: No recipient for package " + packageId);
            return;
        }
    
        float processingCharge = processor.computeCollectionCharge(currentPackage);
        processor.processCollection(currentRecipient, currentPackage);
    
        // Update released items record
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter("released.csv", true)))) {
            writer.println(String.format("%s,%s,%.2f,%s,%s,£%.2f",
                currentRecipient.getSurname(), packageId,
                currentPackage.getMassKg(), currentPackage.getMeasurementSpec(),
                currentPackage.getDeliveryState(), processingCharge));
        } catch (IOException ex) {
            logger.logEvent("Failed to update delivery records: " + ex.getMessage());
        }
    
        // Update system files
        updateSystemRecords(currentRecipient, packageId);
    
        recipientQueue.dequeueRecipient();
    
        System.out.printf("Processed: %s collected package %s. Charge: £%.2f%n",
            currentRecipient.getSurname(), packageId, processingCharge);
    }

    private void updateSystemRecords(Customer recipient, String packageId) {
        // Update Recipients file
        updateFile("Recipients.csv", line -> {
            String[] data = line.split(",");
            return data.length >= 2 && 
                   data[0].trim().equals(recipient.getSurname()) && 
                   data[1].trim().equals(packageId);
        });

        // Update Inventory file
        updateFile("Inventory.csv", line -> {
            String[] data = line.split(",");
            return data.length >= 1 && data[0].trim().equals(packageId);
        });
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
        String recipientName = inputReader.nextLine();
        System.out.print("Enter package identifier: ");
        String packageId = inputReader.nextLine();

        if (packageCollection.getPackageByID(packageId) == null) {
            System.out.println("Error: Package identifier not found in system.");
            logger.logEvent("Registration failed: " + recipientName + " - Package " + packageId + " not found");
            return;
        }

        int queuePosition = recipientQueue.getQueueContents().size() + 1;
        Customer newRecipient = new Customer(recipientName, packageId, queuePosition);
        recipientQueue.enqueueRecipient(newRecipient);

        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter("Recipients.csv", true)))) {
            writer.println(recipientName + "," + packageId);
            logger.logEvent("New recipient registered: " + recipientName + " for package: " + packageId);
        } catch (IOException ex) {
            System.err.println("Failed to update recipient records: " + ex.getMessage());
            logger.logEvent("Failed to register recipient in system: " + ex.getMessage());
        }
    }

    private void registerNewPackage() {
        System.out.print("Enter package identifier: ");
        String identifier = inputReader.nextLine();
        System.out.print("Enter mass (kg): ");
        float mass = Float.parseFloat(inputReader.nextLine());
        System.out.print("Enter measurements (length width height): ");
        String measurements = inputReader.nextLine();

        Parcel newPackage = new Parcel(identifier, mass, measurements);
        packageCollection.addPackage(newPackage);

        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter("Inventory.csv", true)))) {
            String[] dimensions = measurements.split(" ");
            writer.println(identifier + "," + mass + "," + String.join(",", dimensions));
            logger.logEvent("New package registered: " + identifier + 
                          " (Mass: " + mass + "kg, Size: " + measurements + ")");
        } catch (IOException ex) {
            System.err.println("Failed to update inventory records: " + ex.getMessage());
            logger.logEvent("Failed to register package in system: " + ex.getMessage());
        }
    }

    private void deregisterRecipient() {
        System.out.print("Enter recipient name to remove: ");
        String recipientName = inputReader.nextLine();
        System.out.print("Enter package identifier: ");
        String packageId = inputReader.nextLine();

        Parcel associatedPackage = packageCollection.getPackageByID(packageId);
        if (associatedPackage != null && !associatedPackage.getDeliveryState().equals("Collected")) {
            System.out.println("Cannot remove: Package still in depot system");
            logger.logEvent("Deregistration failed: " + recipientName + 
                          " - Package " + packageId + " not yet collected");
            return;
        }

        boolean removalSuccess = false;
        try {
            File originalFile = new File("Recipients.csv");
            File tempFile = new File("Recipients.tmp");

            try (BufferedReader reader = new BufferedReader(new FileReader(originalFile));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    String[] data = line.split(",");
                    if (data.length >= 2 && 
                        data[0].trim().equals(recipientName) && 
                        data[1].trim().equals(packageId)) {
                        removalSuccess = true;
                        continue;
                    }
                    writer.write(line + System.lineSeparator());
                }
            }

            if (removalSuccess) {
                originalFile.delete();
                tempFile.renameTo(originalFile);
                logger.logEvent("Recipient deregistered: " + recipientName + 
                              " with package: " + packageId);
            } else {
                tempFile.delete();
                System.out.println("Recipient not found in system");
                logger.logEvent("Deregistration failed: Recipient " + recipientName + 
                              " not found in records");
            }
        } catch (IOException ex) {
            System.err.println("Failed to update recipient records: " + ex.getMessage());
            logger.logEvent("Error during recipient deregistration: " + ex.getMessage());
        }
    }

    private void removePackage() {
        System.out.print("Enter package identifier to remove: ");
        String packageId = inputReader.nextLine();

        Parcel packageToRemove = packageCollection.getPackageByID(packageId);
        if (packageToRemove == null) {
            System.out.println("Package not found in system");
            logger.logEvent("Removal failed: Package " + packageId + " not found");
            return;
        }

        if (!packageToRemove.getDeliveryState().equals("Collected")) {
            System.out.println("Cannot remove: Package still in depot system");
            logger.logEvent("Removal failed: Package " + packageId + " not yet collected");
            return;
        }

        boolean removalSuccess = false;
        try {
            File originalFile = new File("Inventory.csv");
            File tempFile = new File("Inventory.tmp");

            try (BufferedReader reader = new BufferedReader(new FileReader(originalFile));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    String[] data = line.split(",");
                    if (data.length >= 1 && data[0].trim().equals(packageId)) {
                        removalSuccess = true;
                        continue;
                    }
                    writer.write(line + System.lineSeparator());
                }
            }

            if (removalSuccess) {
                originalFile.delete();
                tempFile.renameTo(originalFile);
                logger.logEvent("Package removed from system: " + packageId);
            } else {
                tempFile.delete();
                System.out.println("Package not found in inventory records");
                logger.logEvent("Removal failed: Package " + packageId + 
                              " not found in inventory records");
            }
        } catch (IOException ex) {
            System.err.println("Failed to update inventory records: " + ex.getMessage());
            logger.logEvent("Error during package removal: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        DepotSystem system = new DepotSystem();
        system.initializeSystem();
        system.startSystem();
    }
}