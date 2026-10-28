package br.finedu.forum;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ForumLikeRepository extends JpaRepository<ForumLike, Long> {

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    Optional<ForumLike> findByPostIdAndUserId(Long postId, Long userId);

    long countByPostId(Long postId);

    @Query("select l.postId, count(l) from ForumLike l where l.postId in :ids group by l.postId")
    List<Object[]> countByPosts(@Param("ids") Collection<Long> ids);

    @Query("select l.postId from ForumLike l where l.userId = :userId and l.postId in :ids")
    List<Long> likedBy(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);
}
