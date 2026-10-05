package com.example.campussecondhand.service;

import com.example.campussecondhand.entity.CommentRecord;
import com.example.campussecondhand.entity.LikeRecord;
import com.example.campussecondhand.entity.Product;
import com.example.campussecondhand.entity.ShareRecord;
import com.example.campussecondhand.entity.User;
import com.example.campussecondhand.repository.CommentRecordRepository;
import com.example.campussecondhand.repository.LikeRecordRepository;
import com.example.campussecondhand.repository.ProductRepository;
import com.example.campussecondhand.repository.ShareRecordRepository;
import com.example.campussecondhand.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PostInteractionService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private LikeRecordRepository likeRecordRepository;

    @Autowired
    private CommentRecordRepository commentRecordRepository;

    @Autowired
    private ShareRecordRepository shareRecordRepository;

    @Autowired
    private UserRepository userRepository;

    public Map<String, Object> toggleLike(Long userId, Long postId) {
        ensurePostExists(postId);
        int liked = likeRecordRepository.countByPostIdAndUserId(postId, userId);
        boolean isLiked;
        if (liked > 0) {
            likeRecordRepository.deleteByPostIdAndUserId(postId, userId);
            isLiked = false;
        } else {
            LikeRecord record = new LikeRecord();
            record.setUserId(userId);
            record.setPostId(postId);
            record.setCreatedTime(LocalDateTime.now());
            likeRecordRepository.insert(record);
            isLiked = true;
        }
        int count = likeRecordRepository.countByPostId(postId);
        Map<String, Object> result = new HashMap<>();
        result.put("liked", isLiked);
        result.put("likeCount", count);
        return result;
    }

    public Map<String, Object> getStats(Long userId, Long postId) {
        ensurePostExists(postId);
        Map<String, Object> result = new HashMap<>();
        result.put("liked", userId != null && likeRecordRepository.countByPostIdAndUserId(postId, userId) > 0);
        result.put("likeCount", likeRecordRepository.countByPostId(postId));
        result.put("commentCount", commentRecordRepository.countByPostId(postId));
        result.put("shareCount", shareRecordRepository.countByPostId(postId));
        return result;
    }

    public List<CommentRecord> listComments(Long postId, int page, int size) {
        ensurePostExists(postId);
        int offset = Math.max(page - 1, 0) * size;
        List<CommentRecord> comments = commentRecordRepository.findByPostId(postId, size, offset);
        comments.forEach(c -> {
            User user = userRepository.selectById(c.getUserId());
            if (user != null) c.setUsername(user.getUsername());
        });
        return comments;
    }

    public CommentRecord addComment(Long userId, Long postId, Long parentId, String content) {
        ensurePostExists(postId);
        CommentRecord comment = new CommentRecord();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setParentId(parentId);
        comment.setContent(content);
        comment.setCreatedTime(LocalDateTime.now());
        commentRecordRepository.insert(comment);
        User user = userRepository.selectById(userId);
        if (user != null) comment.setUsername(user.getUsername());
        return comment;
    }

    public Map<String, Object> share(Long userId, Long postId) {
        ensurePostExists(postId);
        ShareRecord record = new ShareRecord();
        record.setUserId(userId);
        record.setPostId(postId);
        record.setCreatedTime(LocalDateTime.now());
        shareRecordRepository.insert(record);
        Map<String, Object> result = new HashMap<>();
        result.put("shareCount", shareRecordRepository.countByPostId(postId));
        return result;
    }

    private Product ensurePostExists(Long postId) {
        Product product = productRepository.selectById(postId);
        if (product == null) {
            throw new RuntimeException("帖子不存在");
        }
        return product;
    }
}
