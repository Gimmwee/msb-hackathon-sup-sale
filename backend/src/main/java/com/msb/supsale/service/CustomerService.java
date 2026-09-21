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

    @Transactional
    public void updateEmail(String phone, String email) {
        customerRepository.findByPhone(phone).ifPresent(customer -> {
            customer.setEmail(email);
            customerRepository.save(customer);
        });
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
