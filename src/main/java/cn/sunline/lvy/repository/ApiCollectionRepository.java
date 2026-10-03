package cn.sunline.lvy.repository;

import cn.sunline.lvy.entity.ApiCollection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApiCollectionRepository extends JpaRepository<ApiCollection, Long> {

    List<ApiCollection> findAllByOrderByUpdatedAtDesc();

    boolean existsByName(String name);
}
