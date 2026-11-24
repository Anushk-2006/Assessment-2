package Task04;

public class Medicine {
    private final String code;
    private final String name;
    private int stock;
    private final double unitPrice;

    public Medicine(String code, String name, int stock, double unitPrice) {
        this.code = code;
        this.name = name;
        this.stock = stock;
        this.unitPrice = unitPrice;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public double getUnitPrice() { return unitPrice; }

    @Override
    public String toString() {
        return "[" + code + "] " + name + " (Stock: " + stock + ", Rs." + unitPrice + ")";
    }
}
