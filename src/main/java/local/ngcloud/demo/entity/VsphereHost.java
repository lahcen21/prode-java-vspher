package local.ngcloud.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import local.ngcloud.demo.security.PasswordEncryptionConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "vsphere_hosts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VsphereHost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String ip;
    private String username;
    @Convert(converter = PasswordEncryptionConverter.class)
    private String password;
}