package asc.model;

public class CarPart {
    private String  partId;
    private String  name;
    private String  category;
    private String  description;
    private double  price;
    private String  imageUrl;
    private boolean visible;
    private boolean inStock;
    private String  addedBy;
    private String  addedDate;

    public CarPart(String partId, String name, String category,
                   String description, double price,
                   String imageUrl, boolean visible, boolean inStock,
                   String addedBy, String addedDate) {
        this(partId,name,category,description,price,imageUrl,visible,inStock,addedBy,addedDate,0,3);
    }

    public CarPart(String partId, String name, String category,
                   String description, double price,
                   String imageUrl, boolean visible,
                   String addedBy, String addedDate) {
        this(partId,name,category,description,price,imageUrl,visible,true,addedBy,addedDate,0,3);
    }

    /** Full constructor including stockQuantity and lowStockThreshold (for file loading). */
    public CarPart(String partId, String name, String category,
                   String description, double price,
                   String imageUrl, boolean visible, boolean inStock,
                   String addedBy, String addedDate,
                   int stockQuantity, int lowStockThreshold) {
        this.partId=partId; this.name=name; this.category=category;
        this.description=description; this.price=price; this.imageUrl=imageUrl;
        this.visible=visible; this.inStock=inStock;
        this.addedBy=addedBy; this.addedDate=addedDate;
        this.stockQuantity=stockQuantity;
        this.lowStockThreshold=lowStockThreshold;
    }

    public String  getPartId()              { return partId; }
    public String  getName()               { return name; }
    public void    setName(String n)       { this.name=n; }
    public String  getCategory()           { return category; }
    public void    setCategory(String c)   { this.category=c; }
    public String  getDescription()        { return description; }
    public void    setDescription(String d){ this.description=d; }
    public double  getPrice()              { return price; }
    public void    setPrice(double p)      { this.price=p; }
    public String  getImageUrl()           { return imageUrl; }
    public void    setImageUrl(String u)   { this.imageUrl=u; }
    public boolean isVisible()             { return visible; }
    public void    setVisible(boolean v)   { this.visible=v; }
    public boolean isInStock()             { return inStock; }
    public void    setInStock(boolean s)   { this.inStock=s; }
    public String  getAddedBy()            { return addedBy; }
    public String  getAddedDate()          { return addedDate; }
    public String  stockLabel()            { return inStock ? "In Stock" : "Out of Stock"; }

    // ── Stock quantity / low-stock helpers (used by Technician Inventory panel) ──
    private int stockQuantity    = 10;
    private int lowStockThreshold = 3;

    public int     getStockQuantity()           { return stockQuantity; }
    public void    setStockQuantity(int q)      { this.stockQuantity = q; this.inStock = (q > 0); }
    public int     getLowStockThreshold()       { return lowStockThreshold; }
    public void    setLowStockThreshold(int t)  { this.lowStockThreshold = t; }
    public boolean isLowStock()                 { return stockQuantity <= lowStockThreshold; }

    public String toFileString() {
        return partId+"|"+name+"|"+category+"|"
              +description.replace("|","/")+"|"
              +String.format("%.2f",price)+"|"
              +imageUrl+"|"+(visible?"1":"0")+"|"+(inStock?"1":"0")+"|"
              +addedBy+"|"+addedDate+"|"
              +stockQuantity+"|"+lowStockThreshold;
    }
}
