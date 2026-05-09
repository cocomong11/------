package com.example.taxassistant.domain.checklist;

import com.example.taxassistant.domain.enums.ChecklistItemStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, UUID> {

    List<ChecklistItem> findAllByBusinessId(UUID businessId);

    List<ChecklistItem> findAllByBusinessIdAndStatus(UUID businessId, ChecklistItemStatus status);

    void deleteAllByBusinessId(UUID businessId);
}
