package local.ngcloud.demo;

import com.vmware.vim25.mo.*;
import com.vmware.vim25.DatastoreSummary;
import com.vmware.vim25.HostNetworkInfo;
import com.vmware.vim25.HostPortGroup;
import com.vmware.vim25.HostVirtualSwitch;
import local.ngcloud.demo.dto.CloneVMRequestDTO;
import local.ngcloud.demo.dto.HostRegistrationDTO;
import local.ngcloud.demo.dto.DatastoreDTO;
import local.ngcloud.demo.dto.HostNetworkDTO;
import local.ngcloud.demo.dto.HostSystemDTO;
import local.ngcloud.demo.dto.VirtualMachineDTO;
import local.ngcloud.demo.entity.VsphereHost;
import local.ngcloud.demo.repository.VsphereHostRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.rmi.RemoteException;
import java.security.KeyStore;
import jakarta.transaction.Transactional;
import java.net.URL;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import local.ngcloud.demo.strategy.DatastoreSelectionStrategy;
import org.springframework.scheduling.annotation.Async;

import com.vmware.vim25.*; // Import all necessary vSphere API types
@Service
public class VsphereService implements HypervisorService {

    @Value("${vsphere.ip:#{null}}")
    private String defaultIp;
    @Value("${vsphere.username:#{null}}")
    private String defaultUsername;
    @Value("${vsphere.password:#{null}}")
    private String defaultPassword;

    // Propriétés pour la gestion du TrustStore (optionnel)
    @Value("${vsphere.truststore.path:#{null}}") // Default to null if not set
    private String truststorePath;
    @Value("${vsphere.truststore.password:#{null}}") // Default to null if not set
    private String truststorePassword;

    private final DatastoreSelectionStrategy datastoreSelectionStrategy;
    private final VsphereHostRepository vsphereHostRepository;

    public VsphereService(DatastoreSelectionStrategy datastoreSelectionStrategy, VsphereHostRepository vsphereHostRepository) {
        this.datastoreSelectionStrategy = datastoreSelectionStrategy;
        this.vsphereHostRepository = vsphereHostRepository;
    }

    // Registre des sessions actives et des identifiants (pour auto-reconnect)
    private final Map<String, ServiceInstance> sessions = new ConcurrentHashMap<>();
    private final Map<String, HostRegistrationDTO> hostRegistry = new ConcurrentHashMap<>();

    @PostConstruct
    public void initializeConnection() throws Exception {
        // 1. Charger les hôtes persistés en base de données
        List<VsphereHost> persistedHosts = vsphereHostRepository.findAll();
        for (VsphereHost host : persistedHosts) {
            try {
                this.registerInMaps(HostRegistrationDTO.builder().ip(host.getIp()).username(host.getUsername()).password(host.getPassword()).build());
            } catch (Exception e) {
                System.err.println("VsphereService: Erreur lors du chargement de l'hôte " + host.getIp() + " : " + e.getMessage());
            }
        }

        // 2. Gérer l'hôte par défaut du fichier properties
        if (defaultIp != null && !defaultIp.isEmpty()) {
            System.out.println("VsphereService: Auto-enregistrement de l'hôte par défaut " + defaultIp);
            registerHost(HostRegistrationDTO.builder()
                    .ip(defaultIp)
                    .username(defaultUsername)
                    .password(defaultPassword)
                    .build());
        }
    }

    @Override
    @Transactional
    public void registerHost(HostRegistrationDTO reg) throws Exception {
        // Persister ou mettre à jour en base
        VsphereHost host = vsphereHostRepository.findByIp(reg.getIp())
                .orElse(new VsphereHost());
        host.setIp(reg.getIp());
        host.setUsername(reg.getUsername());
        host.setPassword(reg.getPassword());
        vsphereHostRepository.save(host);

        this.registerInMaps(reg);
    }

    private void registerInMaps(HostRegistrationDTO reg) throws Exception {
        System.out.println("VsphereService: Initialisation de la session pour " + reg.getIp());
        ServiceInstance si = connect(reg);
        sessions.put(reg.getIp(), si);
        hostRegistry.put(reg.getIp(), reg);
    }

    @Override
    @Transactional
    public void unregisterHost(String ip) {
        ServiceInstance si = sessions.remove(ip);
        if (si != null) {
            si.getServerConnection().logout();
        }
        hostRegistry.remove(ip);
        vsphereHostRepository.deleteByIp(ip);
        System.out.println("VsphereService: Hôte " + ip + " supprimé.");
    }

    @Override
    public Set<String> getRegisteredHosts() {
        return hostRegistry.keySet();
    }

    private ServiceInstance connect(HostRegistrationDTO reg) throws Exception {
        URL url = new URL("https://" + reg.getIp() + "/sdk");

        TrustManager trustManager = null;

        if (truststorePath != null && !truststorePath.isEmpty()) {
            System.out.println("VsphereService: Tentative d'utilisation du TrustStore personnalisé depuis " + truststorePath);
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (FileInputStream fis = new FileInputStream(truststorePath)) {
                trustStore.load(fis, truststorePassword != null ? truststorePassword.toCharArray() : null);
            }

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            if (tmf.getTrustManagers().length > 0) {
                trustManager = tmf.getTrustManagers()[0];
            }
        } else {
            System.out.println("VsphereService: Ignorance de la validation du certificat SSL (aucun TrustStore configuré).");
        }

        if (trustManager != null) {
            return new ServiceInstance(url, reg.getUsername(), reg.getPassword(), trustManager, 0, 0);
        } else {
            return new ServiceInstance(url, reg.getUsername(), reg.getPassword(), true);
        }
    }

    private ServiceInstance getConnectedServiceInstance(String ip) throws Exception {
        ServiceInstance si = sessions.get(ip);
        if (si == null || isSessionInvalid(si)) {
            synchronized (this) {
                si = sessions.get(ip);
                if (si == null || isSessionInvalid(si)) {
                    HostRegistrationDTO reg = hostRegistry.get(ip);
                    if (reg == null) {
                        throw new RuntimeException("Hôte non enregistré ou IP inconnue : " + ip);
                    }
                    System.out.println("VsphereService: Session expired or not connected. Reconnecting...");
                    si = connect(reg);
                    sessions.put(ip, si);
                }
            }
        }
        return si;
    }

    private boolean isSessionInvalid(ServiceInstance si) {
        if (si == null || si.getServerConnection().getSessionStr() == null) {
            return true;
        }
        try {
            si.currentTime();
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    public HostSystemDTO getHostSystemInfo(String ip) throws Exception {
        ServiceInstance si = getConnectedServiceInstance(ip);
        ManagedEntity[] hosts = new InventoryNavigator(si.getRootFolder()).searchManagedEntities("HostSystem");
        if (hosts == null || hosts.length == 0) throw new RuntimeException("Aucun hôte trouvé pour l'IP : " + ip);

        HostSystem host = (HostSystem) hosts[0];
        HostHardwareSummary hw = host.getSummary().getHardware();
        
        return HostSystemDTO.builder()
                .name(host.getName())
                .vendor(hw.getVendor())
                .model(hw.getModel())
                .osName(si.getAboutInfo().getFullName())
                .apiVersion(si.getAboutInfo().getApiVersion())
                .cpuCores(hw.getNumCpuCores())
                .totalMemoryGB(hw.getMemorySize() / (1024 * 1024 * 1024))
                .build();
    }

    @Override
    public List<VirtualMachineDTO> getAllVirtualMachines(String ip) throws Exception {
        try {
            ServiceInstance si = getConnectedServiceInstance(ip);
            List<VirtualMachineDTO> vmsList = new ArrayList<>();
            Folder rootFolder = si.getRootFolder();
            ManagedEntity[] vms = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");

            if (vms != null) {
                for (ManagedEntity me : vms) {
                    VirtualMachine vm = (VirtualMachine) me;
                    // On filtre pour ne garder que les VMs réelles, pas les templates
                    if (vm.getConfig() != null && vm.getConfig().isTemplate()) {
                        continue;
                    }
                    vmsList.add(VirtualMachineDTO.builder()
                            .name(vm.getName())
                            .powerState(vm.getRuntime().getPowerState().toString())
                            .cpuCount(vm.getConfig().getHardware().getNumCPU())
                            .memoryMB(vm.getConfig().getHardware().getMemoryMB())
                            .guestOS(vm.getConfig().getGuestId())
                            .build());
                }
            }
            return vmsList;
        } catch (RemoteException e) {
            sessions.remove(ip);
            throw e;
        }
    }

    @Override
    public List<VirtualMachineDTO> getAllTemplates(String ip) throws Exception {
        ServiceInstance si = getConnectedServiceInstance(ip);
        List<VirtualMachineDTO> templatesList = new ArrayList<>();
        Folder rootFolder = si.getRootFolder();
        ManagedEntity[] vms = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");

        if (vms != null) {
            for (ManagedEntity me : vms) {
                VirtualMachine vm = (VirtualMachine) me;
                if (vm.getConfig() != null && vm.getConfig().isTemplate()) {
                    templatesList.add(VirtualMachineDTO.builder()
                            .name(vm.getName())
                            .powerState(vm.getRuntime().getPowerState().toString())
                            .cpuCount(vm.getConfig().getHardware().getNumCPU())
                            .memoryMB(vm.getConfig().getHardware().getMemoryMB())
                            .guestOS(vm.getConfig().getGuestId())
                            .build());
                }
            }
        }
        return templatesList;
    }

    @Override
    public List<DatastoreDTO> getAllDatastores(String ip) throws Exception {
        ServiceInstance si = getConnectedServiceInstance(ip);
        List<DatastoreDTO> datastores = new ArrayList<>();
        ManagedEntity[] entities = new InventoryNavigator(si.getRootFolder()).searchManagedEntities("Datastore");

        if (entities != null) {
            for (ManagedEntity me : entities) {
                Datastore ds = (Datastore) me;
                DatastoreSummary summary = ds.getSummary();
                long capacity = summary.getCapacity();
                long freeSpace = summary.getFreeSpace();
                datastores.add(DatastoreDTO.builder()
                        .name(ds.getName())
                        .capacityGB(capacity / (1024 * 1024 * 1024))
                        .freeSpaceGB(freeSpace / (1024 * 1024 * 1024))
                        .type(ds.getSummary().getType())
                        .build());
            }
        }
        return datastores;
    }

    public DatastoreDTO getRecommendedDatastore(String ip) throws Exception {
        List<DatastoreDTO> allDatastores = getAllDatastores(ip);
        return datastoreSelectionStrategy.selectDatastore(allDatastores)
                .orElseThrow(() -> new RuntimeException("Aucun datastore trouvé pour la sélection automatique."));
    }

    @Override
    public HostNetworkDTO getHostNetworking(String ip) throws Exception {
        ServiceInstance si = getConnectedServiceInstance(ip);
        // Sur un ESXi standalone, on récupère le premier hôte trouvé
        ManagedEntity[] hosts = new InventoryNavigator(si.getRootFolder()).searchManagedEntities("HostSystem");
        if (hosts == null || hosts.length == 0) return null;

        HostSystem host = (HostSystem) hosts[0];
        HostNetworkInfo netInfo = host.getConfig().getNetwork();

        List<String> pgNames = netInfo.getPortgroup() == null ? new ArrayList<>() : 
                Arrays.stream(netInfo.getPortgroup())
                .map(HostPortGroup::getSpec)
                .map(spec -> spec.getName())
                .collect(Collectors.toList());

        List<String> vSwitchNames = netInfo.getVswitch() == null ? new ArrayList<>() : 
                Arrays.stream(netInfo.getVswitch())
                .map(HostVirtualSwitch::getName)
                .collect(Collectors.toList());

        List<String> pNicNames = netInfo.getPnic() == null ? new ArrayList<>() : 
                Arrays.stream(netInfo.getPnic())
                .map(p -> p.getDevice())
                .collect(Collectors.toList());

        return HostNetworkDTO.builder()
                .portGroups(pgNames)
                .virtualSwitches(vSwitchNames)
                .physicalNics(pNicNames)
                .build();
    }

    @Override
    @Async
    public CompletableFuture<Void> powerOnVM(String ip, String vmName) throws Exception {
        ServiceInstance si = getConnectedServiceInstance(ip);
        VirtualMachine vm = (VirtualMachine) new InventoryNavigator(si.getRootFolder())
                .searchManagedEntity("VirtualMachine", vmName);
        
        if (vm == null) throw new RuntimeException("VM non trouvée : " + vmName);
        
        Task task = vm.powerOnVM_Task(null);
        task.waitForTask(); // On attend la fin pour la démo, à passer en @Async plus tard
        return CompletableFuture.completedFuture(null);
    }

    @Override
    @Async
    public CompletableFuture<Void> powerOffVM(String ip, String vmName) throws Exception {
        ServiceInstance si = getConnectedServiceInstance(ip);
        VirtualMachine vm = (VirtualMachine) new InventoryNavigator(si.getRootFolder())
                .searchManagedEntity("VirtualMachine", vmName);
        
        if (vm == null) throw new RuntimeException("VM non trouvée : " + vmName);
        
        Task task = vm.powerOffVM_Task();
        task.waitForTask();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    @Async
    public CompletableFuture<Void> deleteVM(String ip, String vmName) throws Exception {
        ServiceInstance si = getConnectedServiceInstance(ip);
        VirtualMachine vm = (VirtualMachine) new InventoryNavigator(si.getRootFolder())
                .searchManagedEntity("VirtualMachine", vmName);

        if (vm == null) throw new RuntimeException("VM non trouvée : " + vmName);

        // Une VM doit être éteinte pour être supprimée du disque
        if (vm.getRuntime().getPowerState() != VirtualMachinePowerState.poweredOff) {
            throw new RuntimeException("La VM '" + vmName + "' doit être éteinte pour être supprimée.");
        }

        Task task = vm.destroy_Task();
        task.waitForTask();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    @Async
    public CompletableFuture<VirtualMachineDTO> cloneVM(String ip, CloneVMRequestDTO request) throws Exception {
        ServiceInstance si = getConnectedServiceInstance(ip);
        VirtualMachine sourceVm = (VirtualMachine) new InventoryNavigator(si.getRootFolder())
                .searchManagedEntity("VirtualMachine", request.getTemplateName());

        if (sourceVm == null) throw new RuntimeException("Source non trouvée : " + request.getTemplateName());

        Datastore ds = (Datastore) new InventoryNavigator(si.getRootFolder())
                .searchManagedEntity("Datastore", request.getDatastoreName());
        if (ds == null) throw new RuntimeException("Datastore non trouvé : " + request.getDatastoreName());

        // Préparation du clonage (Utilisation des champs publics pour éviter les conflits de types JAXB)
        VirtualMachineCloneSpec cloneSpec = new VirtualMachineCloneSpec();
        VirtualMachineRelocateSpec relocateSpec = new VirtualMachineRelocateSpec();
        relocateSpec.datastore = ds.getMOR();
        relocateSpec.host = sourceVm.getRuntime().getHost();
        
        cloneSpec.location = relocateSpec;
        cloneSpec.powerOn = false;
        cloneSpec.template = false;

        // Reconfiguration à la volée durant le clone
        if (request.getCpuCount() > 0 || request.getMemoryMB() > 0) {
            VirtualMachineConfigSpec configSpec = new VirtualMachineConfigSpec();
            if (request.getCpuCount() > 0) configSpec.numCPUs = request.getCpuCount();
            if (request.getMemoryMB() > 0) configSpec.memoryMB = (long) request.getMemoryMB();
            cloneSpec.config = configSpec;
        }

        System.out.println("VsphereService: Début du clonage asynchrone pour " + request.getNewVmName());
        Task task = sourceVm.cloneVM_Task((Folder) sourceVm.getParent(), request.getNewVmName(), cloneSpec);
        task.waitForTask();

        VirtualMachine newVm = (VirtualMachine) new InventoryNavigator(si.getRootFolder())
                .searchManagedEntity("VirtualMachine", request.getNewVmName());

        return CompletableFuture.completedFuture(VirtualMachineDTO.builder()
                .name(newVm.getName())
                .powerState(newVm.getRuntime().getPowerState().toString())
                .cpuCount(newVm.getConfig().getHardware().getNumCPU())
                .memoryMB(newVm.getConfig().getHardware().getMemoryMB())
                .guestOS(newVm.getConfig().getGuestId())
                .build());
    }

    @Override
    @Async
    public CompletableFuture<Void> reconfigureVM(String ip, String vmName, int cpuCount, int memoryMB) throws Exception {
        ServiceInstance si = getConnectedServiceInstance(ip);
        VirtualMachine vm = (VirtualMachine) new InventoryNavigator(si.getRootFolder())
                .searchManagedEntity("VirtualMachine", vmName);

        if (vm == null) throw new RuntimeException("VM non trouvée : " + vmName);

        VirtualMachineConfigSpec configSpec = new VirtualMachineConfigSpec();
        if (cpuCount > 0) configSpec.numCPUs = cpuCount;
        if (memoryMB > 0) configSpec.memoryMB = (long) memoryMB;

        System.out.println("VsphereService: Reconfiguration à chaud de " + vmName);
        Task task = vm.reconfigVM_Task(configSpec);
        task.waitForTask();
        return CompletableFuture.completedFuture(null);
    }

    @PreDestroy
    public void disconnect() {
        System.out.println("VsphereService: Fermeture de toutes les sessions actives...");
        sessions.forEach((ip, si) -> {
            si.getServerConnection().logout();
        });
        sessions.clear();
        hostRegistry.clear();
    }
}