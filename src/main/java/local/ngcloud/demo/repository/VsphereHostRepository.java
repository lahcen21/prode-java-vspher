package local.ngcloud.demo.repository;

import local.ngcloud.demo.entity.VsphereHost;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VsphereHostRepository extends JpaRepository<VsphereHost, Long> {
    Optional<VsphereHost> findByIp(String ip);
    void deleteByIp(String ip);
}