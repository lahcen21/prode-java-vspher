package local.ngcloud.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HostSystemDTO {
    private String name;
    private String vendor;
    private String model;
    private String osName;
    private String apiVersion;
    private int cpuCores;
    private long totalMemoryGB;
}