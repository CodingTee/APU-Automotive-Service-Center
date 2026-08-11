package asc.model;

import java.util.*;

public class ServicePrice {
    private double normalServicePrice;
    private double majorServicePrice;
    private String normalDescription;
    private String majorDescription;
    private List<String> normalIncludedPartIds;  // comma-separated part IDs
    private List<String> majorIncludedPartIds;

    public ServicePrice(double normalServicePrice, double majorServicePrice) {
        this(normalServicePrice, majorServicePrice,
             "Standard 1-hour service including oil change and safety check.",
             "Comprehensive 3-hour service for full vehicle maintenance.",
             new ArrayList<>(), new ArrayList<>());
    }

    public ServicePrice(double normalServicePrice, double majorServicePrice,
                        String normalDescription, String majorDescription,
                        List<String> normalIncludedPartIds, List<String> majorIncludedPartIds) {
        this.normalServicePrice     = normalServicePrice;
        this.majorServicePrice      = majorServicePrice;
        this.normalDescription      = normalDescription  != null ? normalDescription  : "";
        this.majorDescription       = majorDescription   != null ? majorDescription   : "";
        this.normalIncludedPartIds  = normalIncludedPartIds  != null ? normalIncludedPartIds  : new ArrayList<>();
        this.majorIncludedPartIds   = majorIncludedPartIds   != null ? majorIncludedPartIds   : new ArrayList<>();
    }

    public double getNormalServicePrice() { return normalServicePrice; }
    public void   setNormalServicePrice(double p) { this.normalServicePrice = p; }
    public double getMajorServicePrice()  { return majorServicePrice; }
    public void   setMajorServicePrice(double p)  { this.majorServicePrice = p; }

    public String getNormalDescription() { return normalDescription; }
    public void   setNormalDescription(String d) { this.normalDescription = d != null ? d : ""; }
    public String getMajorDescription()  { return majorDescription; }
    public void   setMajorDescription(String d)  { this.majorDescription  = d != null ? d : ""; }

    public List<String> getNormalIncludedPartIds() { return normalIncludedPartIds; }
    public void setNormalIncludedPartIds(List<String> ids) { this.normalIncludedPartIds = ids != null ? ids : new ArrayList<>(); }
    public List<String> getMajorIncludedPartIds()  { return majorIncludedPartIds; }
    public void setMajorIncludedPartIds(List<String> ids)  { this.majorIncludedPartIds  = ids != null ? ids : new ArrayList<>(); }

    public double getPriceForService(String serviceType) {
        return "NORMAL".equalsIgnoreCase(serviceType) ? normalServicePrice : majorServicePrice;
    }

    public String getDescriptionForService(String serviceType) {
        return "NORMAL".equalsIgnoreCase(serviceType) ? normalDescription : majorDescription;
    }

    public List<String> getIncludedPartIdsForService(String serviceType) {
        return "NORMAL".equalsIgnoreCase(serviceType) ? normalIncludedPartIds : majorIncludedPartIds;
    }

    // File format:
    // normalPrice|majorPrice|normalDesc|majorDesc|normalPartIds(semicolon)|majorPartIds(semicolon)
    public String toFileString() {
        String nParts = String.join(";", normalIncludedPartIds);
        String mParts = String.join(";", majorIncludedPartIds);
        String nDesc  = normalDescription.replace("|", "/").replace("\n", "\\n");
        String mDesc  = majorDescription .replace("|", "/").replace("\n", "\\n");
        return normalServicePrice + "|" + majorServicePrice + "|" + nDesc + "|" + mDesc + "|" + nParts + "|" + mParts;
    }

    public static ServicePrice fromFileString(String line) {
        try {
            String[] p = line.split("\\|", 6);
            double nPrice = Double.parseDouble(p[0].trim());
            double mPrice = Double.parseDouble(p[1].trim());
            String nDesc  = p.length > 2 ? p[2].replace("\\n", "\n") : "";
            String mDesc  = p.length > 3 ? p[3].replace("\\n", "\n") : "";
            List<String> nIds = p.length > 4 && !p[4].trim().isEmpty()
                    ? new ArrayList<>(Arrays.asList(p[4].trim().split(";"))) : new ArrayList<>();
            List<String> mIds = p.length > 5 && !p[5].trim().isEmpty()
                    ? new ArrayList<>(Arrays.asList(p[5].trim().split(";"))) : new ArrayList<>();
            return new ServicePrice(nPrice, mPrice, nDesc, mDesc, nIds, mIds);
        } catch (Exception e) {
            return new ServicePrice(100.0, 250.0);
        }
    }
}
