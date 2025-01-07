public class Parcel {
    private String packageID;
    private int storageDuration;
    private float massKg;
    private String measurementSpec;
    private String deliveryState;
    private float collectionCharge;
    
    public Parcel(String packageID, float massKg, String measurementSpec) {
        this.packageID = packageID;
        this.massKg = massKg;
        this.measurementSpec = measurementSpec;
        this.storageDuration = 0;
        this.deliveryState = "Pending";
        this.collectionCharge = 0.0f;
    }
    
    public void updateStorageDuration() {
        storageDuration++;
    }
    
    public void setDeliveryState(String state) {
        this.deliveryState = state;
    }
    
    public float computeCollectionCharge() {
        return collectionCharge;
    }
    
    public void markAsCollected() {
        this.deliveryState = "Collected";
    }
    
    // Getters
    public String getPackageID() { return packageID; }
    public float getMassKg() { return massKg; }
    public String getMeasurementSpec() { return measurementSpec; }
    public int getStorageDuration() { return storageDuration; }
    public String getDeliveryState() { return deliveryState; }
    
    @Override
    public String toString() {
        return String.format("Package[ID=%s, Mass=%.2f, Measurements=%s, Duration=%d, State=%s]",
                packageID, massKg, measurementSpec, storageDuration, deliveryState);
    }
}