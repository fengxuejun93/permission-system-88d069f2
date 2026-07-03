package com.campus.config;

import com.campus.model.*;
import com.campus.service.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MockDataInitializer implements CommandLineRunner {

    private final UserService userService;
    private final PostService postService;
    private final FriendService friendService;
    private final CommentService commentService;
    private final LikeService likeService;

    public MockDataInitializer(UserService userService, PostService postService,
                               FriendService friendService, CommentService commentService,
                               LikeService likeService) {
        this.userService = userService;
        this.postService = postService;
        this.friendService = friendService;
        this.commentService = commentService;
        this.likeService = likeService;
    }

    @Override
    public void run(String... args) {
        // 创建模拟同学
        userService.save(new User(1L, "zhangsan", "张三",
                "https://trae-api-cn.mchort.guru/api/ide/v1/text_to_image?prompt=avatar%20of%20a%20chinese%20college%20boy%20smiling%20headshot%20clean%20background&image_size=square_hd",
                "计算机2023级1班", true, "单身", "喜欢打篮球和写代码"));

        userService.save(new User(2L, "lisi", "李四",
                "https://trae-api-cn.mchort.guru/api/ide/v1/text_to_image?prompt=avatar%20of%20a%20chinese%20college%20girl%20with%20glasses%20headshot&image_size=square_hd",
                "英语2023级2班", true, "恋爱中", "读书和旅行是我的最爱"));

        userService.save(new User(3L, "wangwu", "王五",
                "https://trae-api-cn.mchort.guru/api/ide/v1/text_to_image?prompt=avatar%20of%20a%20chinese%20college%20boy%20with%20short%20hair%20headshot&image_size=square_hd",
                "数学2023级1班", false, "单身", "数学竞赛选手"));

        userService.save(new User(4L, "zhaoliu", "赵六",
                "https://trae-api-cn.mchort.guru/api/ide/v1/text_to_image?prompt=avatar%20of%20a%20chinese%20college%20girl%20long%20hair%20headshot&image_size=square_hd",
                "计算机2023级1班", true, "单身", "前端开发爱好者"));

        userService.save(new User(5L, "sunqi", "孙七",
                "https://trae-api-cn.mchort.guru/api/ide/v1/text_to_image?prompt=avatar%20of%20a%20chinese%20college%20boy%20with%20cap%20headshot&image_size=square_hd",
                "物理2023级1班", false, "恋爱中", "物理实验达人"));

        // 好友关系：张三-李四(已通过), 张三-赵六(已通过)
        FriendRequest r1 = friendService.addRequest(2L, 1L);
        friendService.acceptRequest(r1.getId());
        FriendRequest r2 = friendService.addRequest(1L, 4L);
        friendService.acceptRequest(r2.getId());

        // 待处理申请：王五申请加张三
        friendService.addRequest(3L, 1L);

        // 模拟动态（PUBLISHED + 各种可见性）
        Post p1 = postService.createPost(new Post(null, 1L, "今天校园的樱花开了，好美！",
                Visibility.PUBLIC, PostStatus.PUBLISHED, List.of(
                "https://trae-api-cn.mchort.guru/api/ide/v1/text_to_image?prompt=cherry%20blossom%20trees%20on%20a%20chinese%20university%20campus%20spring&image_size=landscape_16_9")));

        Post p2 = postService.createPost(new Post(null, 2L, "图书馆的自习室太抢手了，早上六点就满了 😤",
                Visibility.PUBLIC, PostStatus.PUBLISHED, List.of()));

        Post p3 = postService.createPost(new Post(null, 4L, "新写了一个小项目，开心！仅好友可见哦~",
                Visibility.FRIENDS, PostStatus.PUBLISHED, List.of(
                "https://trae-api-cn.mchort.guru/api/ide/v1/text_to_image?prompt=computer%20screen%20showing%20code%20on%20desk%20cozy%20room&image_size=landscape_16_9")));

        Post p4 = postService.createPost(new Post(null, 1L, "自己记录一下今天的心情日记。",
                Visibility.PRIVATE, PostStatus.PUBLISHED, List.of()));

        Post p5 = postService.createPost(new Post(null, 5L, "物理实验终于通过了！",
                Visibility.PUBLIC, PostStatus.PUBLISHED, List.of(
                "https://trae-api-cn.mchort.guru/api/ide/v1/text_to_image?prompt=physics%20laboratory%20experiment%20equipment%20university&image_size=landscape_16_9")));

        Post p6 = postService.createPost(new Post(null, 3L, "今天解了一道好难的积分题~仅好友可见",
                Visibility.FRIENDS, PostStatus.PUBLISHED, List.of()));

        // 草稿动态
        Post p7 = postService.createPost(new Post(null, 1L, "草稿：下周的社团活动计划，还没写完...",
                Visibility.PUBLIC, PostStatus.DRAFT, List.of()));

        Post p8 = postService.createPost(new Post(null, 2L, "草稿：读后感还没写完",
                Visibility.FRIENDS, PostStatus.DRAFT, List.of()));

        // 已隐藏动态
        Post p9 = postService.createPost(new Post(null, 1L, "之前发的照片不太好看，先隐藏了",
                Visibility.PUBLIC, PostStatus.HIDDEN, List.of(
                "https://trae-api-cn.mchort.guru/api/ide/v1/text_to_image?prompt=sunset%20over%20university%20campus%20building&image_size=landscape_16_9")));

        // 模拟点赞
        likeService.addInitialLike(p1.getId(), 2L);
        likeService.addInitialLike(p1.getId(), 4L);
        likeService.addInitialLike(p1.getId(), 1L);
        likeService.addInitialLike(p2.getId(), 1L);
        likeService.addInitialLike(p5.getId(), 1L);
        likeService.addInitialLike(p5.getId(), 2L);

        // 模拟评论
        Comment c1 = commentService.addComment(p1.getId(), 1L, null, "樱花确实美！我也去看了");
        Comment c2 = commentService.addComment(p1.getId(), 2L, null, "在哪？我也想去看");
        commentService.addComment(p1.getId(), 1L, c2.getId(), "就在图书馆后面那条路上~");
        commentService.addComment(p2.getId(), 1L, null, "哈哈哈确实，我都是五点半去排队的");
        commentService.addComment(p5.getId(), 1L, null, "恭喜！太厉害了");
        commentService.addComment(p5.getId(), 2L, null, "物理实验好难");
        Comment c7 = commentService.addComment(p5.getId(), 5L, null, "谢谢大家");
        commentService.addComment(p5.getId(), 1L, c7.getId(), "继续加油！");
    }
}
