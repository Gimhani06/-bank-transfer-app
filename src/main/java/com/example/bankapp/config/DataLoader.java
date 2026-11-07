package com.example.bankapp.config;

import com.example.bankapp.model.Customer;
import com.example.bankapp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final CustomerRepository customerRepository;

    @Override
    public void run(String... args) {
        // Insert sample customers only if the database table is empty
        if (customerRepository.count() == 0) {
            customerRepository.save(new Customer(null, "100001", "Kamal Perera", "kamal@gmail.com", new BigDecimal("50000.00")));
            customerRepository.save(new Customer(null, "100002", "Nimali Silva", "nimali@gmail.com", new BigDecimal("25000.00")));
            customerRepository.save(new Customer(null, "100003", "Saman Kumara", "saman@gmail.com", new BigDecimal("10000.00")));
        }
    }
}