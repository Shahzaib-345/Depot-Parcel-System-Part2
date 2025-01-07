import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

public class PackageCollection {
    private Map<String, Parcel> parcels;
    
    public PackageCollection() {
        parcels = new HashMap<>();
    }
    
    public boolean addPackage(Parcel par) {
        if (!parcels.containsKey(par.getPackageID())) {
            parcels.put(par.getPackageID(), par);
            return true;
        }
        return false;
    }
    
    public Parcel getPackageByID(String packageID) {
        return parcels.get(packageID);
    }
    
    public boolean removePackage(String packageID) {
        return parcels.remove(packageID) != null;
    }
    
    public Collection<Parcel> getAllPackages() {
        return parcels.values();
    }
}