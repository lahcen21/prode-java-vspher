package local.ngcloud.demo.strategy;

import local.ngcloud.demo.dto.DatastoreDTO;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class MostFreeSpaceStrategy implements DatastoreSelectionStrategy {

    @Override
    public Optional<DatastoreDTO> selectDatastore(List<DatastoreDTO> datastores) {
        return datastores.stream()
                .max(Comparator.comparingLong(DatastoreDTO::getFreeSpaceGB));
    }
}
