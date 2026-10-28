package br.finedu.forum;

import br.finedu.forum.ForumEntities.Status;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ForumCommentRepository extends JpaRepository<ForumComment, Long> {

    Page<ForumComment> findByPostIdAndStatusOrderByCreatedAtAsc(Long postId, Status status, Pageable pageable);

    Page<ForumComment> findByStatusOrderByUpdatedAtDesc(Status status, Pageable pageable);

    @Query("select c.postId, count(c) from ForumComment c where c.postId in :ids and c.status = :status group by c.postId")
    List<Object[]> countByPosts(@Param("ids") Collection<Long> ids, @Param("status") Status status);

    List<ForumComment> findByAuthorIdOrderByCreatedAtAsc(Long authorId);
}
