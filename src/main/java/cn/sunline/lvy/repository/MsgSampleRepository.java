package cn.sunline.lvy.repository;

import cn.sunline.lvy.entity.MsgSample;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MsgSampleRepository extends JpaRepository<MsgSample, Long> {

    boolean existsByTransIdAndSourceIpAndMessageType(String transId, String sourceIp, String messageType);

    Page<MsgSample> findByServiceCodeOrderByCreatedAtDesc(String serviceCode, Pageable pageable);

    Page<MsgSample> findByServiceCodeContainingOrTransIdContainingOrderByCreatedAtDesc(
            String serviceCode, String transId, Pageable pageable);

    Page<MsgSample> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
