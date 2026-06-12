package com.example.rankinggame.repositories;

import com.example.rankinggame.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
