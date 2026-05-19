package com.smartfinancepty.finance.repository.attachment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.smartfinancepty.finance.domain.attachment.FileAttachment;

@Repository
public interface FileAttachmentRepository extends JpaRepository<FileAttachment, Long> {

    List<FileAttachment> findByUserIdAndActiveTrue(Long userId);

    List<FileAttachment> findByExpenseIdAndActiveTrue(Long expenseId);

    Optional<FileAttachment> findByIdAndUserId(Long id, Long userId);

    List<FileAttachment> findByUserIdAndExpenseIsNullAndActiveTrue(Long userId);
}
