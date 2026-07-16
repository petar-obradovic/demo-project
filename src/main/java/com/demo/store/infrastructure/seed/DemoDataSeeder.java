package com.demo.store.infrastructure.seed;

import com.demo.store.domain.customer.Customer;
import com.demo.store.domain.customer.CustomerRepository;
import com.demo.store.domain.product.Product;
import com.demo.store.domain.product.ProductRepository;
import com.demo.store.domain.shared.Address;
import com.demo.store.domain.shared.Email;
import com.demo.store.domain.shared.Money;
import com.demo.store.infrastructure.mongo.SpringDataCustomerRepository;
import com.demo.store.infrastructure.mongo.SpringDataProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final SpringDataProductRepository productDocuments;
    private final SpringDataCustomerRepository customerDocuments;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    public DemoDataSeeder(SpringDataProductRepository productDocuments,
                          SpringDataCustomerRepository customerDocuments,
                          ProductRepository productRepository,
                          CustomerRepository customerRepository) {
        this.productDocuments = productDocuments;
        this.customerDocuments = customerDocuments;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
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
}
