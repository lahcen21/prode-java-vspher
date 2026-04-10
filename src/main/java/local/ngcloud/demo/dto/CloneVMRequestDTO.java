package local.ngcloud.demo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloneVMRequestDTO {
    @NotBlank(message = "Le nom du template/source est obligatoire")
    private String templateName;

    @NotBlank(message = "Le nom de la nouvelle VM est obligatoire")
    @Size(min = 3, max = 80, message = "Le nom de la VM doit faire entre 3 et 80 caractères")
    private String newVmName;

    @NotBlank(message = "Le nom du datastore de destination est obligatoire")
    private String datastoreName;

    @Min(value = 1, message = "Le nombre de CPU doit être d'au moins 1")
    private int cpuCount;

    @Min(value = 512, message = "La mémoire RAM doit être d'au moins 512 MB")
    private int memoryMB;
}