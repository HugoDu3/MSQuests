package com.github.ibanetchep.msquests.core.repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PoolCooldownRepository {
    CompletableFuture<Optional<Long>> getLastUse(UUID actorId, String poolId);
    CompletableFuture<Void> updateLastUse(UUID actorId, String poolId, long timestamp);
}
