package com.github.ibanetchep.msquests.database.repository;

import com.github.ibanetchep.msquests.core.repository.PoolCooldownRepository;
import com.github.ibanetchep.msquests.database.DbAccess;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PoolCooldownSqlRepository extends SqlRepository implements PoolCooldownRepository {

    public PoolCooldownSqlRepository(DbAccess dbAccess) {
        super(dbAccess);
    }

    @Override
    public CompletableFuture<Optional<Long>> getLastUse(UUID actorId, String poolId) {
        String query = "SELECT last_use FROM msquests_pool_cooldown WHERE actor_id = :actorId AND pool_id = :poolId";

        return supplyAsync(() -> getJdbi().withHandle(handle ->
                handle.createQuery(query)
                        .bind("actorId", actorId.toString())
                        .bind("poolId", poolId)
                        .mapTo(Long.class)
                        .findOne()
        ));
    }

    @Override
    public CompletableFuture<Void> updateLastUse(UUID actorId, String poolId, long timestamp) {
        String query = """
            INSERT INTO msquests_pool_cooldown (actor_id, pool_id, last_use)
            VALUES (:actorId, :poolId, :lastUse)
            ON DUPLICATE KEY UPDATE last_use = :lastUse
        """;

        return runAsync(() -> getJdbi().useHandle(handle ->
                handle.createUpdate(query)
                        .bind("actorId", actorId.toString())
                        .bind("poolId", poolId)
                        .bind("lastUse", new Timestamp(timestamp))
                        .execute()
        ));
    }
}
