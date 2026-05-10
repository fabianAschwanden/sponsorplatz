package ch.sponsorplatz.projekt;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedienAssetRepository extends JpaRepository<MedienAsset, UUID> {

    List<MedienAsset> findByEntityTypAndEntityIdOrderBySortierungAsc(EntityTyp entityTyp, UUID entityId);

    Optional<MedienAsset> findFirstByEntityTypAndEntityIdAndAssetTypOrderBySortierungAsc(
            EntityTyp entityTyp, UUID entityId, AssetTyp assetTyp);

    long countByEntityTypAndEntityId(EntityTyp entityTyp, UUID entityId);

    List<MedienAsset> findByEntityTypAndEntityIdAndAssetTypOrderBySortierungAsc(
            EntityTyp entityTyp, UUID entityId, AssetTyp assetTyp);
}

