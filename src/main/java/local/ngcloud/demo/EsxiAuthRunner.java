package local.ngcloud.demo;

import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;
import java.net.URL;

public class EsxiAuthRunner {

    public static void main(String[] args) {
        String esxiIp = "192.168.1.18"; 
        String user = "root";
        String password = "UHP-fsts2023";
        ServiceInstance si = null;

        try {
            // Connexion simplifiée avec YaviJava
            // URL format: https://IP/sdk
            URL url = new URL("https://" + esxiIp + "/sdk");
            
            // Le 4ème paramètre 'true' permet d'ignorer la validation SSL
            si = new ServiceInstance(url, user, password, true);

            System.out.println("✅ Authentification réussie avec YaviJava !");
            System.out.println("Serveur : " + si.getAboutInfo().getFullName());
            
            // Exploration de l'inventaire
            Folder rootFolder = si.getRootFolder();
            ManagedEntity[] vms = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");

            System.out.println("\n--- Liste des Machines Virtuelles ---");
            if (vms == null || vms.length == 0) {
                System.out.println("Aucune VM trouvée.");
            } else {
                for (ManagedEntity entity : vms) {
                    VirtualMachine vm = (VirtualMachine) entity;
                    System.out.printf("- %s [Etat: %s, CPU: %d vCPUs, RAM: %d MB]%n", 
                        vm.getName(), 
                        vm.getRuntime().getPowerState(),
                        vm.getConfig().getHardware().getNumCPU(),
                        vm.getConfig().getHardware().getMemoryMB());
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur : " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (si != null) {
                // Déconnexion propre pour libérer les ressources sur l'ESXi
                si.getServerConnection().logout();
            }
        }
    }
}