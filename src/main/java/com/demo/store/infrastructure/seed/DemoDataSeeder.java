package com.demo.store.infrastructure.seed;

import com.demo.store.domain.customer.Customer;
import com.demo.store.domain.customer.CustomerRepository;
import com.demo.store.domain.customer.CustomerId;
import com.demo.store.domain.order.Order;
import com.demo.store.domain.order.OrderLine;
import com.demo.store.domain.order.OrderRepository;
import com.demo.store.domain.product.Product;
import com.demo.store.domain.product.ProductRepository;
import com.demo.store.domain.shared.Address;
import com.demo.store.domain.shared.Email;
import com.demo.store.domain.shared.Money;
import com.demo.store.infrastructure.mongo.SpringDataCustomerRepository;
import com.demo.store.infrastructure.mongo.SpringDataOrderRepository;
import com.demo.store.infrastructure.mongo.SpringDataProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final String DEMO_CUSTOMER_ID = "bbab9618-9dda-44ce-8b8e-1bd000462f06";

    private final SpringDataProductRepository productDocuments;
    private final SpringDataCustomerRepository customerDocuments;
    private final SpringDataOrderRepository orderDocuments;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    public DemoDataSeeder(SpringDataProductRepository productDocuments,
                          SpringDataCustomerRepository customerDocuments,
                          SpringDataOrderRepository orderDocuments,
                          ProductRepository productRepository,
                          CustomerRepository customerRepository,
                          OrderRepository orderRepository) {
        this.productDocuments = productDocuments;
        this.customerDocuments = customerDocuments;
        this.orderDocuments = orderDocuments;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public void run(String... args) {
        if (productDocuments.count() == 0) {
            seedProducts();
            log.info("Seeded demo products");
        }
        if (customerDocuments.count() == 0) {
            seedCustomers();
            log.info("Seeded demo customers");
        }
        if (orderDocuments.findByCustomerId(DEMO_CUSTOMER_ID).isEmpty()) {
            seedOrders();
            log.info("Seeded demo orders for customer {}", DEMO_CUSTOMER_ID);
        }
    }

    private void seedProducts() {
        List<Product> products = List.of(
                Product.create("LAP-13", "Aurora 13 Laptop", "13-inch ultrabook, 16 GB RAM", Money.of("999.90"), 25),
                Product.create("LAP-15", "Aurora 15 Laptop", "15-inch workstation, 32 GB RAM", Money.of("1499.00"), 12),
                Product.create("MON-27", "PixelView 27 Monitor", "27-inch 4K IPS monitor", Money.of("329.99"), 40),
                Product.create("MOU-01", "GlidePro Mouse", "Wireless ergonomic mouse", Money.of("19.99"), 200),
                Product.create("KEY-75", "TactiType 75 Keyboard", "75% mechanical keyboard", Money.of("89.50"), 80),
                Product.create("HUB-07", "PortMax 7-in-1 Hub", "USB-C hub: HDMI, 3xUSB-A, SD", Money.of("45.00"), 150),
                Product.create("HDS-90", "SilentFocus Headset", "ANC over-ear headset", Money.of("129.99"), 60),
                Product.create("CAM-4K", "ClearCast 4K Webcam", "4K webcam with privacy shutter", Money.of("79.90"), 90),
                Product.create("SSD-1T", "SwiftDrive 1TB SSD", "NVMe M.2 SSD, 1 TB", Money.of("99.00"), 120),
                Product.create("DCK-02", "DeskDock Stand", "Aluminium laptop stand", Money.of("39.99"), 75),
                Product.create("CBL-2M", "DuraLink USB-C Cable 2m", "100W braided USB-C cable", Money.of("12.50"), 300),
                Product.create("BAG-15", "CommuterShell Backpack", "15-inch laptop backpack", Money.of("59.00"), 45));
        products.forEach(productRepository::save);
    }

    private void seedCustomers() {
        customerRepository.save(Customer.register("Ana Petrovic",
                new Email("ana.petrovic@example.com"),
                new Address("Kneza Milosa 12", "Belgrade", "11000", "RS")));
        customerRepository.save(Customer.register("Marko Ilic",
                new Email("marko.ilic@example.com"),
                new Address("Bulevar Oslobodjenja 45", "Novi Sad", "21000", "RS")));
    }

    private void seedOrders() {
        CustomerId customerId = new CustomerId(DEMO_CUSTOMER_ID);
        List<Product> products = productRepository.findAllActive();

        // Order 1 — laptop + mouse, status NEW
        Product laptop = findBySku(products, "LAP-13");
        Product mouse  = findBySku(products, "MOU-01");
        OrderLine line1 = new OrderLine(laptop.id(), laptop.name(), laptop.price(), 1,
                laptop.price());
        OrderLine line2 = new OrderLine(mouse.id(), mouse.name(), mouse.price(), 2,
                mouse.price().multiply(2));
        Money total1 = laptop.price().add(mouse.price().multiply(2));
        orderRepository.save(Order.place(customerId, List.of(line1, line2), total1));

        // Order 2 — monitor + hub + cable, status PAID
        Product monitor = findBySku(products, "MON-27");
        Product hub     = findBySku(products, "HUB-07");
        Product cable   = findBySku(products, "CBL-2M");
        OrderLine line3 = new OrderLine(monitor.id(), monitor.name(), monitor.price(), 1,
                monitor.price());
        OrderLine line4 = new OrderLine(hub.id(), hub.name(), hub.price(), 1,
                hub.price());
        OrderLine line5 = new OrderLine(cable.id(), cable.name(), cable.price(), 3,
                cable.price().multiply(3));
        Money total2 = monitor.price().add(hub.price()).add(cable.price().multiply(3));
        Order order2 = Order.place(customerId, List.of(line3, line4, line5), total2);
        order2.markPaid();
        orderRepository.save(order2);

        // Order 3 — keyboard + headset + webcam, status DELIVERED
        Product keyboard = findBySku(products, "KEY-75");
        Product headset  = findBySku(products, "HDS-90");
        Product webcam   = findBySku(products, "CAM-4K");
        OrderLine line6 = new OrderLine(keyboard.id(), keyboard.name(), keyboard.price(), 1,
                keyboard.price());
        OrderLine line7 = new OrderLine(headset.id(), headset.name(), headset.price(), 1,
                headset.price());
        OrderLine line8 = new OrderLine(webcam.id(), webcam.name(), webcam.price(), 1,
                webcam.price());
        Money total3 = keyboard.price().add(headset.price()).add(webcam.price());
        Order order3 = Order.place(customerId, List.of(line6, line7, line8), total3);
        order3.markPaid();
        order3.markShipped();
        order3.markDelivered();
        orderRepository.save(order3);
    }

    private Product findBySku(List<Product> products, String sku) {
        return products.stream()
                .filter(p -> p.sku().equals(sku))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Seeded product not found: " + sku));
    }
}
