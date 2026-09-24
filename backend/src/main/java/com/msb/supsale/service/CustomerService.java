package com.msb.supsale.service;

import com.msb.supsale.dto.CccdDto;
import com.msb.supsale.model.Customer;
import com.msb.supsale.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CustomerService {
    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Optional<Customer> findByPhone(String phone) {
        return customerRepository.findByPhone(phone);
    }

    public Optional<Customer> findByIdNumber(String idNumber) {
        return customerRepository.findByIdNumber(idNumber);
    }

    public Optional<Customer> findByName(String name) {
        return customerRepository.findByNameIgnoreCase(name);
    }

    public Optional<Customer> search(String query) {
        Optional<Customer> byPhone = customerRepository.findByPhone(query);
        if (byPhone.isPresent()) return byPhone;
        Optional<Customer> byId = customerRepository.findByIdNumber(query);
        if (byId.isPresent()) return byId;
        return customerRepository.findByNameIgnoreCase(query);
    }

    @Transactional
    public void updateEmail(String phone, String email) {
        Customer customer = customerRepository.findByPhone(phone).orElseGet(() -> {
            Customer c = new Customer();
            c.setPhone(phone);
            c.setName("Unknown");
            return c;
        });
        customer.setEmail(email);
        customerRepository.save(customer);
    }

    @Transactional
    public Customer upsertFromLead(String phone, String name, String email) {
        Customer customer = customerRepository.findByPhone(phone).orElseGet(() -> {
            Customer c = new Customer();
            c.setPhone(phone);
            return c;
        });
        if (name != null && !name.isBlank()) customer.setName(name);
        if (email != null && !email.isBlank()) customer.setEmail(email);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer saveOrUpdateFromCccd(String phone, CccdDto dto) {
        Optional<Customer> existing = customerRepository.findByPhone(phone);
        Customer customer;
        if (existing.isPresent()) {
            customer = existing.get();
        } else {
            customer = new Customer();
            customer.setPhone(phone);
            customer.setName(dto.getFullName() != null ? dto.getFullName() : "Unknown");
        }
        if (dto.getFullName() != null) customer.setName(dto.getFullName());
        customer.setIdNumber(dto.getIdNumber());
        customer.setDob(dto.getDob());
        customer.setGender(dto.getGender());
        customer.setAddress(dto.getAddress());
        return customerRepository.save(customer);
    }
}
