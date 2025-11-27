package pl.nop.aiplayers.storage;

import pl.nop.aiplayers.model.AIPlayerProfile;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AIPlayerStorage {

    private final File dataFolder;

    public AIPlayerStorage(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    public void saveAll(Collection<AIPlayerProfile> profiles) {
        // TODO: implement persistence to disk
    }

    public List<AIPlayerProfile> loadAll() {
        // TODO: load profiles from disk
        return Collections.emptyList();
    }
}
