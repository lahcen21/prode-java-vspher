package local.ngcloud.demo;

import local.ngcloud.demo.dto.CloneVMRequestDTO;
import local.ngcloud.demo.dto.DatastoreDTO;
import local.ngcloud.demo.dto.HostNetworkDTO;
import local.ngcloud.demo.dto.HostSystemDTO;
import local.ngcloud.demo.dto.VirtualMachineDTO;
import local.ngcloud.demo.dto.HostRegistrationDTO;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface HypervisorService {
    void registerHost(HostRegistrationDTO registration) throws Exception;
    void unregisterHost(String ip);
    Set<String> getRegisteredHosts();

    HostSystemDTO getHostSystemInfo(String ip) throws Exception;
    List<VirtualMachineDTO> getAllVirtualMachines(String ip) throws Exception;
    List<DatastoreDTO> getAllDatastores(String ip) throws Exception;
    HostNetworkDTO getHostNetworking(String ip) throws Exception;
    List<VirtualMachineDTO> getAllTemplates(String ip) throws Exception;
    
    CompletableFuture<Void> powerOnVM(String ip, String vmName) throws Exception;
    CompletableFuture<Void> powerOffVM(String ip, String vmName) throws Exception;
    CompletableFuture<Void> deleteVM(String ip, String vmName) throws Exception;

    CompletableFuture<VirtualMachineDTO> cloneVM(String ip, CloneVMRequestDTO request) throws Exception;
    CompletableFuture<Void> reconfigureVM(String ip, String vmName, int cpuCount, int memoryMB) throws Exception;
}