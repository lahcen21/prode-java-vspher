package local.ngcloud.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VirtualMachineDTO {
    private String name;
    private String powerState;
    private int cpuCount;
    private int memoryMB;
    private String guestOS;
}