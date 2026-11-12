package com.oficioya.worker.integration;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface UserDirectoryClient {

    // Devuelve solo los ids que Users reporta como activos; los demás no aparecen
    Set<UUID> findActiveIds(Collection<UUID> userIds);
}