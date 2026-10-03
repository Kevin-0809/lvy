package cn.sunline.lvy.repository;

import cn.sunline.lvy.entity.RequestHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestHistoryRepository extends JpaRepository<RequestHistory, Long> {

    Page<RequestHistory> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
