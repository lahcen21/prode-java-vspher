package local.ngcloud.demo.strategy;

import local.ngcloud.demo.dto.DatastoreDTO;
import java.util.List;
import java.util.Optional;

public interface DatastoreSelectionStrategy {
    Optional<DatastoreDTO> selectDatastore(List<DatastoreDTO> datastores);
}