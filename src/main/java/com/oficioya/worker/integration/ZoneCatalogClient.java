package com.oficioya.worker.integration;

import java.util.Set;

public interface ZoneCatalogClient {

    // true si todos los ids existen en Users; lanza ExternalServiceUnavailableException si Users no responde
    boolean existAll(Set<Long> zoneIds);
}
