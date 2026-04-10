package local.ngcloud.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HostNetworkDTO {
    private List<String> portGroups;
    private List<String> physicalNics;
    private List<String> virtualSwitches;
}
