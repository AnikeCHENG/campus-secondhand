package com.example.campussecondhand.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.campussecondhand.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PostRepository extends BaseMapper<Post> {

    /**
     * 某用户的动态总数（hall-profile 的 stats.posts）。
     *
     * <p>用 COUNT 聚合而非查回集合在内存里数，避免把全部动态加载进 JVM。</p>
     */
    @Select("SELECT COUNT(*) FROM posts WHERE user_id = #{userId}")
    long countByUser(@Param("userId") Long userId);

    /** 某用户指定类型的动态数（hall-profile 的 stats.seeking 只用 SEEK） */
    @Select("SELECT COUNT(*) FROM posts WHERE user_id = #{userId} AND type = #{type}")
    long countByUserAndType(@Param("userId") Long userId, @Param("type") String type);

    /**
     * 批量取点赞数：一次 IN 查询覆盖整页动态，避免逐条 COUNT 的 N+1。
     *
     * <p>只对商品类动态传入 productId 列表；非商品类动态不在结果里，
     * 由 Service 层保持 0。</p>
     */
    @Select("<script>" +
            "SELECT post_id AS targetId, COUNT(*) AS cnt FROM like_record " +
            "WHERE post_id IN <foreach collection='ids' item='it' open='(' separator=',' close=')'>#{it}</foreach> " +
            "GROUP BY post_id" +
            "</script>")
    List<CountRow> countLikesByTargets(@Param("ids") List<Long> ids);

    /** 批量取评论数，同样只对商品类动态生效 */
    @Select("<script>" +
            "SELECT post_id AS targetId, COUNT(*) AS cnt FROM comments " +
            "WHERE post_id IN <foreach collection='ids' item='it' open='(' separator=',' close=')'>#{it}</foreach> " +
            "GROUP BY post_id" +
            "</script>")
    List<CountRow> countCommentsByTargets(@Param("ids") List<Long> ids);

    /** 批量取分享数 */
    @Select("<script>" +
            "SELECT post_id AS targetId, COUNT(*) AS cnt FROM share_record " +
            "WHERE post_id IN <foreach collection='ids' item='it' open='(' separator=',' close=')'>#{it}</foreach> " +
            "GROUP BY post_id" +
            "</script>")
    List<CountRow> countSharesByTargets(@Param("ids") List<Long> ids);

    /** 聚合结果行：targetId 即互动表里的 post_id（当前语义为 product_id） */
    class CountRow {
        private Long targetId;
        private Long cnt;

        public Long getTargetId() { return targetId; }
        public void setTargetId(Long targetId) { this.targetId = targetId; }
        public Long getCnt() { return cnt; }
        public void setCnt(Long cnt) { this.cnt = cnt; }
    }
}