package com.github.ibanetchep.msquests.database.repository;

import com.github.ibanetchep.msquests.core.repository.ChainProgressRepository;
import com.github.ibanetchep.msquests.database.DbAccess;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ChainProgressSqlRepository extends SqlRepository implements ChainProgressRepository {

    public ChainProgressSqlRepository(DbAccess dbAccess) {
        super(dbAccess);
    }

    @Override
    public CompletableFuture<Optional<Integer>> getCurrentIndex(UUID actorId, String chainId) {
        String query = "SELECT current_index FROM msquests_chain_progress WHERE actor_id = :actorId AND chain_id = :chainId";
        return supplyAsync(() ->
                getJdbi().withHandle(h ->
                        h.createQuery(query)
                                .bind("actorId", actorId.toString())
                                .bind("chainId", chainId)
                                .mapTo(Integer.class)
                                .findOne()
                )
        );
    }

    @Override
    public CompletableFuture<Void> updateCurrentIndex(UUID actorId, String chainId, int index) {
        String query = """
            INSERT INTO msquests_chain_progress(actor_id,chain_id,current_index)
            VALUES(:actorId,:chainId,:ci)
            ON DUPLICATE KEY UPDATE current_index = :ci
        """;
        return runAsync(() ->
                getJdbi().useHandle(h ->
                        h.createUpdate(query)
                                .bind("actorId", actorId.toString())
                                .bind("chainId", chainId)
                                .bind("ci", index)
                                .execute()
                )
        );
    }
}
