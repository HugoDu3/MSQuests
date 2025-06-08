package com.github.ibanetchep.msquests.core.repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ChainProgressRepository {
    CompletableFuture<Optional<Integer>> getCurrentIndex(UUID actorId, String chainId);
    CompletableFuture<Void> updateCurrentIndex(UUID actorId, String chainId, int index);
}
