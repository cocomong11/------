package com.example.taxassistant.domain.category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRuleRepository extends JpaRepository<CategoryRule, UUID> {

    List<CategoryRule> findAllByBusinessId(UUID businessId);

    List<CategoryRule> findAllByBusinessIdAndActiveTrue(UUID businessId);

    Optional<CategoryRule> findFirstByBusinessIdAndKeywordIgnoreCaseAndActiveTrue(UUID businessId, String keyword);
}
