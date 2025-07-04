import java.util.*;

interface Shippable {
    String getName();
    double getWeight();
}

abstract class Product {
    protected String name;
    protected double price;
    protected int quantity;

    public Product(String name, double price, int quantity) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }

    public boolean isAvailable(int amount) {
        return amount <= quantity;
    }

    public void reduceStock(int amount) {
        quantity -= amount;
    }

    public abstract boolean isExpired();
}

class NonExpirableProduct extends Product {
    public NonExpirableProduct(String name, double price, int quantity) {
        super(name, price, quantity);
    }
    public boolean isExpired() {
        return false;
    }
}

class ExpirableProduct extends Product {
    private Date expiryDate;
    public ExpirableProduct(String name, double price, int quantity, Date expiryDate) {
        super(name, price, quantity);
        this.expiryDate = expiryDate;
    }
    public boolean isExpired() {
        return new Date().after(expiryDate);
    }
}

class ShippableProduct extends NonExpirableProduct implements Shippable {
    private double weight;
    public ShippableProduct(String name, double price, int quantity, double weight) {
        super(name, price, quantity);
        this.weight = weight;
    }
    public double getWeight() {
        return weight;
    }
}

class ShippableExpirableProduct extends ExpirableProduct implements Shippable {
    private double weight;
    public ShippableExpirableProduct(String name, double price, int quantity, Date expiryDate, double weight) {
        super(name, price, quantity, expiryDate);
        this.weight = weight;
    }
    public double getWeight() {
        return weight;
    }
}

class Customer {
    String name;
    double balance;
    public Customer(String name, double balance) {
        this.name = name;
        this.balance = balance;
    }
    public boolean canAfford(double amount) {
        return balance >= amount;
    }
    public void pay(double amount) {
        balance -= amount;
    }
    public double getBalance() {
        return balance;
    }
}

class CartItem {
    Product product;
    int quantity;
    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }
    public double getTotal() {
        return product.getPrice() * quantity;
    }
}

class Cart {
    private List<CartItem> items = new ArrayList<>();

    public void add(Product product, int quantity) {
        if (!product.isAvailable(quantity)) {
            System.out.println("Not enough stock for: " + product.getName());
            return;
        }
        items.add(new CartItem(product, quantity));
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public List<CartItem> getItems() {
        return items;
    }

    public double getSubtotal() {
        double sum = 0;
        for (CartItem i : items) sum += i.getTotal();
        return sum;
    }

    public List<Shippable> getItemsToShip() {
        List<Shippable> ship = new ArrayList<>();
        for (CartItem i : items) {
            if (i.product instanceof Shippable) {
                for (int j = 0; j < i.quantity; j++) {
                    ship.add((Shippable) i.product);
                }
            }
        }
        return ship;
    }
}

class ShippingService {
    public static void ship(List<Shippable> items) {
        if (items.isEmpty()) return;
        System.out.println("** Shipment notice **");
        Map<String, Integer> grouped = new LinkedHashMap<>();
        double totalWeight = 0;
        for (Shippable s : items) {
            grouped.put(s.getName(), grouped.getOrDefault(s.getName(), 0) + 1);
            totalWeight += s.getWeight();
        }
        for (String n : grouped.keySet()) {
            System.out.println(grouped.get(n) + "x " + n);
        }
        System.out.printf("Total package weight %.1fkg%n", totalWeight);
    }
}

class CheckoutService {
    static final double SHIPPING_FEE = 30;

    public static void checkout(Customer customer, Cart cart) {
        if (cart.isEmpty()) {
            System.out.println("Cart is empty. Add something first.");
            return;
        }

        for (CartItem i : cart.getItems()) {
            if (i.product.isExpired()) {
                System.out.println(i.product.getName() + " is expired. Can't checkout.");
                return;
            }
        }

        double subtotal = cart.getSubtotal();
        List<Shippable> ship = cart.getItemsToShip();
        double shipping = ship.isEmpty() ? 0 : SHIPPING_FEE;
        double total = subtotal + shipping;

        if (!customer.canAfford(total)) {
            System.out.println("Not enough balance to complete checkout.");
            return;
        }

        ShippingService.ship(ship);
        customer.pay(total);
        for (CartItem i : cart.getItems()) {
            i.product.reduceStock(i.quantity);
        }

        System.out.println("** Checkout receipt **");
        for (CartItem i : cart.getItems()) {
            System.out.printf("%dx %s %.0f%n", i.quantity, i.product.getName(), i.getTotal());
        }
        System.out.println("----------------------");
        System.out.printf("Subtotal %.0f%n", subtotal);
        System.out.printf("Shipping %.0f%n", shipping);
        System.out.printf("Amount %.0f%n", total);
        System.out.printf("Balance %.0f%n", customer.getBalance());
        System.out.println("END.");
    }
}

public class FawryEcommerceChallenge {
    public static void main(String[] args) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        Product cheese = new ShippableExpirableProduct("Cheese 200g", 100, 5, sdf.parse("2025-12-31"), 0.2);
        Product biscuits = new ShippableExpirableProduct("Biscuits 700g", 150, 3, sdf.parse("2025-12-31"), 0.9);
        Product tv = new ShippableProduct("TV", 300, 2, 10.0);
        Product scratchCard = new NonExpirableProduct("Mobile scratch card", 50, 10);

        Customer customer = new Customer("Sondos", 1000);

        Cart cart = new Cart();
        cart.add(cheese, 2);
        cart.add(biscuits, 1);
        cart.add(scratchCard, 1);

        CheckoutService.checkout(customer, cart);
    }
}