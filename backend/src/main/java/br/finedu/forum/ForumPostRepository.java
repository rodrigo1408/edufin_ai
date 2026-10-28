package br.finedu.forum;

import br.finedu.forum.ForumEntities.Status;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ForumPostRepository extends JpaRepository<ForumPost, Long>, JpaSpecificationExecutor<ForumPost> {

    Optional<ForumPost> findByIdAndStatus(Long id, Status status);

    Page<ForumPost> findByStatusOrderByUpdatedAtDesc(Status status, Pageable pageable);

    long countByAuthorId(Long authorId);

    java.util.List<ForumPost> findByAuthorIdOrderByCreatedAtAsc(Long authorId);
}
