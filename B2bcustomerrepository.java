package com.shelfwise.repository;

import com.shelfwise.entity.B2BCustomer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface B2BCustomerRepository extends JpaRepository<B2BCustomer, Long> {
}
