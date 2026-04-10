package local.ngcloud.demo;

import local.ngcloud.demo.dto.CloneVMRequestDTO;
import local.ngcloud.demo.dto.DatastoreDTO;
import local.ngcloud.demo.dto.HostNetworkDTO;
import local.ngcloud.demo.dto.HostSystemDTO;
import local.ngcloud.demo.dto.HostRegistrationDTO;
import local.ngcloud.demo.dto.VirtualMachineDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/vsphere")
@Tag(name = "vSphere Control Plane", description = "Gestion dynamique multi-hôtes ESXi et cycle de vie des VMs")
public class VsphereController {

    private final HypervisorService hypervisorService;

    public VsphereController(HypervisorService hypervisorService) {
        this.hypervisorService = hypervisorService;
    }

    @PostMapping("/hosts")
    public String registerHost(@Valid @RequestBody HostRegistrationDTO registration) throws Exception {
        hypervisorService.registerHost(registration);
        return "Hôte " + registration.getIp() + " enregistré avec succès.";
    }

    @DeleteMapping("/hosts/{ip}")
    public String unregisterHost(@PathVariable String ip) {
        hypervisorService.unregisterHost(ip);
        return "Hôte " + ip + " supprimé.";
    }

    @GetMapping("/hosts")
    public Set<String> getHosts() {
        return hypervisorService.getRegisteredHosts();
    }

    @Operation(summary = "Informations système de l'hôte", description = "Récupère les détails matériels (CPU, RAM, Modèle) de l'ESXi.")
    @GetMapping("/{ip}/system")
    public HostSystemDTO getSystemInfo(@PathVariable String ip) throws Exception {
        return hypervisorService.getHostSystemInfo(ip);
    }

    @GetMapping("/{ip}/vms")
    public List<VirtualMachineDTO> getVirtualMachines(@PathVariable String ip) throws Exception {
        return hypervisorService.getAllVirtualMachines(ip);
    }

    @GetMapping("/{ip}/templates")
    public List<VirtualMachineDTO> getTemplates(@PathVariable String ip) throws Exception {
        return hypervisorService.getAllTemplates(ip);
    }

    @GetMapping("/{ip}/datastores")
    public List<DatastoreDTO> getDatastores(@PathVariable String ip) throws Exception {
        return hypervisorService.getAllDatastores(ip);
    }

    @GetMapping("/{ip}/network")
    public HostNetworkDTO getNetworkInfo(@PathVariable String ip) throws Exception {
        return hypervisorService.getHostNetworking(ip);
    }

    @PostMapping("/{ip}/vms/{vmName}/power-on")
    public CompletableFuture<String> powerOnVm(@PathVariable String ip, @PathVariable String vmName) throws Exception {
        return hypervisorService.powerOnVM(ip, vmName)
                .thenApply(v -> "VM " + vmName + " est en cours de démarrage.");
    }

    @PostMapping("/{ip}/vms/{vmName}/power-off")
    public CompletableFuture<String> powerOffVm(@PathVariable String ip, @PathVariable String vmName) throws Exception {
        return hypervisorService.powerOffVM(ip, vmName)
                .thenApply(v -> "VM " + vmName + " est en cours d'arrêt.");
    }

    @DeleteMapping("/{ip}/vms/{vmName}")
    public CompletableFuture<String> deleteVm(@PathVariable String ip, @PathVariable String vmName) throws Exception {
        return hypervisorService.deleteVM(ip, vmName)
                .thenApply(v -> "VM " + vmName + " a été supprimée avec succès.");
    }

    @Operation(summary = "Cloner une VM ou un Template", description = "Crée une nouvelle VM à partir d'une source existante.")
    @PostMapping("/{ip}/vms/clone")
    public CompletableFuture<VirtualMachineDTO> cloneVm(@PathVariable String ip, @Valid @RequestBody CloneVMRequestDTO request) throws Exception {
        return hypervisorService.cloneVM(ip, request);
    }

    @Operation(summary = "Reconfigurer une VM", description = "Modifie le nombre de CPU ou la mémoire RAM d'une VM existante.")
    @PostMapping("/{ip}/vms/{vmName}/reconfigure")
    public CompletableFuture<String> reconfigureVm(
            @PathVariable String ip, 
            @PathVariable String vmName,
            @RequestParam(required = false, defaultValue = "0") int cpuCount,
            @RequestParam(required = false, defaultValue = "0") int memoryMB) throws Exception {
        return hypervisorService.reconfigureVM(ip, vmName, cpuCount, memoryMB)
                .thenApply(v -> "VM " + vmName + " reconfigurée avec succès.");
    }
}