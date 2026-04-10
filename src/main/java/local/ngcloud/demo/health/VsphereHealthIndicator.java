package local.ngcloud.demo.health;

import local.ngcloud.demo.HypervisorService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component("vsphere") 
public class VsphereHealthIndicator implements HealthIndicator {

    private final HypervisorService hypervisorService;

    public VsphereHealthIndicator(HypervisorService hypervisorService) {
        this.hypervisorService = hypervisorService;
    }

    @Override
    public Health health() {
        Set<String> hosts = hypervisorService.getRegisteredHosts();
        if (hosts.isEmpty()) {
            return Health.up().withDetail("message", "Aucun hôte ESXi enregistré.").build();
        }

        Health.Builder status = Health.up();
        for (String ip : hosts) {
            try {
                hypervisorService.getAllVirtualMachines(ip);
                status.withDetail(ip, "UP - Session active");
            } catch (Exception e) {
                status.down().withDetail(ip, "DOWN - " + e.getMessage());
            }
        }
        return status.build();
    }
}