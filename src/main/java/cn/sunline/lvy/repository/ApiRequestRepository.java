package cn.sunline.lvy.repository;

import cn.sunline.lvy.entity.ApiRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApiRequestRepository extends JpaRepository<ApiRequest, Long> {

    List<ApiRequest> findByCollectionIdOrderByUpdatedAtDesc(Long collectionId);

    long countByCollectionId(Long collectionId);

    void deleteByCollectionId(Long collectionId);
}
