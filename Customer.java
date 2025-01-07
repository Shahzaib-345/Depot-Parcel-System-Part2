public class Customer {
    private int sequenceNumber;
    private String surname;
    private String packageID;
    private boolean collectedPackage;
    
    public Customer(String surname, String packageID, int sequenceNumber) {
        this.surname = surname;
        this.packageID = packageID;
        this.sequenceNumber = sequenceNumber;
        this.collectedPackage = false;
    }
    
    public String getSurname() { return surname; }
    public String getPackageID() { return packageID; }
    public int getSequenceNumber() { return sequenceNumber; }
    public boolean hasCollectedPackage() { return collectedPackage; }
    
    public void markPackageCollected() {
        this.collectedPackage = true;
    }
    
    @Override
    public String toString() {
        return String.format("Recipient[Name=%s, PackageID=%s, Sequence=%d]",
                surname, packageID, sequenceNumber);
    }
}