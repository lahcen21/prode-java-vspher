package local.ngcloud.demo.factory;

import local.ngcloud.demo.dto.CloneVMRequestDTO;
import local.ngcloud.demo.dto.VmSize;
import org.springframework.stereotype.Component;

@Component
public class VmConfigurationFactory {

    public CloneVMRequestDTO createConfiguration(VmSize size, String templateName, String newVmName, String datastoreName) {
        switch (size) {
            case SMALL:
                return CloneVMRequestDTO.builder()
                        .templateName(templateName)
                        .newVmName(newVmName)
                        .datastoreName(datastoreName)
                        .cpuCount(1)
                        .memoryMB(2048)
                        .build();
            case MEDIUM:
                return CloneVMRequestDTO.builder()
                        .templateName(templateName)
                        .newVmName(newVmName)
                        .datastoreName(datastoreName)
                        .cpuCount(2)
                        .memoryMB(4096)
                        .build();
            case LARGE:
                return CloneVMRequestDTO.builder()
                        .templateName(templateName)
                        .newVmName(newVmName)
                        .datastoreName(datastoreName)
                        .cpuCount(4)
                        .memoryMB(8192)
                        .build();
            default:
                throw new IllegalArgumentException("Taille de VM inconnue : " + size);
        }
    }
}
