package com.varunu28.thunderingherd.repository;

import com.varunu28.thunderingherd.model.Product;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends CrudRepository<Product, UUID> {
}
