package local.ngcloud.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatastoreDTO {
    private String name;
    private long capacityGB;
    private long freeSpaceGB;
    private String type; // VMFS, NFS, etc.
}
