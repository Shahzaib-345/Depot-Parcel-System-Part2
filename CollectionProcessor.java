public class CollectionProcessor {
    private static final float BASE_CHARGE = 12.50f;
    private static final float MASS_MULTIPLIER = 0.75f;
    private static final float DURATION_CHARGE = 1.25f;

    public float computeCollectionCharge(Parcel pkg) {
        float totalCharge = BASE_CHARGE;
        totalCharge += pkg.getMassKg() * MASS_MULTIPLIER;
        totalCharge += pkg.getStorageDuration() * DURATION_CHARGE;
        return totalCharge;
    }

    public void processCollection(Customer recipient, Parcel pkg) {
        if (pkg != null && !recipient.hasCollectedPackage()) {
            float charge = computeCollectionCharge(pkg);
            pkg.markAsCollected();
            recipient.markPackageCollected();
            EventLogger.getInstance().logEvent(
                String.format("Collection processed: Recipient %s collected package %s. Charge: £%.2f",
                    recipient.getSurname(), pkg.getPackageID(), charge)
            );
        }
    }
}