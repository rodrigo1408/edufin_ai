package br.finedu.portfolio;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletMovementRepository extends JpaRepository<WalletMovement, Long> {

    List<WalletMovement> findByWalletIdOrderByOccurredOnAscIdAsc(Long walletId);

    Page<WalletMovement> findByWalletIdOrderByOccurredOnDescIdDesc(Long walletId, Pageable pageable);

    boolean existsByWalletIdAndReferencePeriod(Long walletId, String referencePeriod);
}
