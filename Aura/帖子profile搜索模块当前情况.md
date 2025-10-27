# 帖子和个人中心相关业务链路设计文档

## 📋 涉及文件清单

### 🎯 帖子相关文件
1. **FeedFragment.java** - 帖子列表显示碎片
2. **CreateFragment.java** - 创建帖子碎片
3. **PostDetailActivity.java** - 帖子详情活动
4. **PostAdapter.java** - 帖子适配器
5. **BookmarksFragment.java** - 书签帖子碎片
6. **MyPostsFragment.java** - 我的帖子碎片
7. **CommentsAdapter.java** - 评论适配器
8. **FeedViewModel.java** - 帖子视图模型
9. **AppRepository.java** - 数据仓库（帖子数据）
10. **Post.java** - 帖子数据模型
11. **MainActivity.java** - 主导航（帖子相关导航）

### 👤 个人中心相关文件
1. **ProfileFragment.java** - 个人中心碎片
2. **ProfilePagerAdapter.java** - 个人中心分页适配器
3. **UserProfile.java** - 用户档案模型
4. **MainActivity.java** - 主导航（个人中心导航）

### 🌐 网络和认证相关文件
1. **AuthManager.java** - 认证管理器
2. **AuraRepository.java** - API仓库层
3. **ApiService.java** - API接口定义
4. **UserProfileResponse.java** - 用户档案响应模型
5. **UserProfileUpdateRequest.java** - 用户档案更新请求模型

---

## 🔗 帖子业务链路分析

## 🔗 帖子业务链路分析

### 1. 📱 帖子列表展示链路
**涉及文件：** `FeedFragment.java`, `PostAdapter.java`, `FeedViewModel.java`, `PostRepository.java`, `Post.java`

-当前链路交互逻辑：用户进入Posts导航页 → FeedFragment初始化 → FeedViewModel通过PostRepository.listPosts()调用后端API获取帖子列表 → 后端返回PostCardResponse列表 → ViewModel转换数据为Post模型 → FeedFragment观察LiveData更新RecyclerView → PostAdapter渲染帖子卡片 → 支持下拉刷新（SwipeRefreshLayout） → 支持上拉加载更多（监听RecyclerView滚动，每页20条） → 支持点击查看详情、点赞、收藏

-实现细节：
a. ✅ 下拉刷新：使用SwipeRefreshLayout实现，调用FeedViewModel.refreshPosts()重置cursor为null并重新加载；b. ✅ 上拉加载更多：通过RecyclerView.OnScrollListener监听，当滚动到倒数第3个时触发loadMorePosts()；c. ✅ 分页参数：每页大小20条（pageSize=20），使用cursor-based分页；d. ✅ 图片显示：使用GlideUtils.loadImage()统一处理URL/文件路径/资源ID等；e. ✅ 互动状态：使用PostInteractionManager本地缓存like/bookmark状态，立即更新UI并异步同步后端；f. ✅ 数据源：已从假数据AppRepository切换到真实后端API（PostRepository）

-代码位置：
```40:232:Aura/app/src/main/java/com/aura/starter/FeedFragment.java
FeedFragment - 主界面实现
```
```13:204:Aura/app/src/main/java/com/aura/starter/FeedViewModel.java
FeedViewModel - 数据管理
```

-链路和后端集成，给出后端交互API接口建议（开发2.0阶段考虑）
```java
@GET("posts")
Call<ApiResponse<PostListResponse>> getPosts(
    @Query("page") Integer page,
    @Query("size") Integer size,
    @Query("sort") String sort  // "latest", "popular", "following"
);
```

### 2. ✏️ 帖子创建链路
**涉及文件：** `CreateFragment.java`, `CreatePostViewModel.java`, `PostRepository.java`, `FileRepository.java`

-当前链路交互逻辑：用户点击FAB按钮 → 进入CreateFragment → 使用CreatePostViewModel管理状态 → 填写标题/内容（实时字数统计） → 选择标签（5个预定义标签：fitness/diet/recipe/plan/outcome，多选） → 选择/拍摄图片（相册选择ActivityResultContracts.GetContent + 相机拍照ActivityResultContracts.TakePicture） → Glide加载预览 → 自动保存草稿到SharedPreferences → 点击发布按钮 → 先上传图片到MinIO（FileRepository） → 成功后调用PostRepository.createPost() → 清空草稿 → 跳转MainActivity并finish当前界面 → 首页自动刷新显示新帖子

-表单验证实现：✅ 标题字数统计显示（无需5-20词限制，只显示当前词数）；✅ 内容至少3个单词；✅ 至少选择一个标签；✅ 满足条件时发布按钮enabled，否则disabled

-标签系统实现：✅ 5个TextView作为标签按钮；✅ 点击切换selected状态并更新背景色；✅ 选中状态保存到CreatePostViewModel；✅ 显示选中的标签列表

-图片上传实现：✅ 选择器弹窗（Gallery/Camera二选一）；✅ 相机权限检查（PermissionManager）；✅ Glide加载URI预览；✅ 不压缩为200x200（使用Glide的centerCrop自适应显示）；✅ 支持删除图片重选

-草稿功能实现：✅ 自动保存到SharedPreferences（etitle、content、tags变化时触发autoSaveDraft）；✅ Fragment重建时自动加载草稿（loadDraft()延迟100ms执行避免数据覆盖）；✅ 清空草稿通过SharedPreferences.clear()

-发布流程：✅ 检查表单有效性 → ✅ 上传图片到MinIO获取URL → ✅ 构建PostCreateRequest（title/content/tags/mediaList） → ✅ 调用PostRepository.createPost() → ✅ 成功后清空草稿和表单 → ✅ 跳转MainActivity并finish

-代码位置：
```57:894:Aura/app/src/main/java/com/aura/starter/CreateFragment.java
CreateFragment - 完整的帖子创建实现
```

-可能依赖或关联的链路编号；2.1（草稿保存）、6.1（我的帖子管理）、1.1（帖子列表展示）、3.1（帖子详情查看）

-链路和后端集成，给出后端交互API接口建议（开发2.0阶段考虑）
```java
@POST("posts")
@Multipart
Call<ApiResponse<PostDto>> createPost(
    @Part MultipartBody.Part image,  // 可选，压缩后的200x200正方形图片
    @Part("title") RequestBody title,  // 5-20个单词，必填
    @Part("content") RequestBody content,  // 不少于5个单词，必填
    @Part("tags") RequestBody tags,  // JSON数组，至少包含一个标签
    @Part("visibility") RequestBody visibility  // "public", "friends", "private"
);

@POST("posts/draft")
@Multipart
Call<ApiResponse<PostDraftDto>> saveDraft(
    @Part MultipartBody.Part image,  // 可选，临时图片文件
    @Part("title") RequestBody title,
    @Part("content") RequestBody content,
    @Part("tags") RequestBody tags,
    @Part("isTemp") RequestBody isTemp  // true表示临时草稿
);

@GET("posts/draft/{draftId}")
Call<ApiResponse<PostDraftDto>> getDraft(@Path("draftId") String draftId);

@DELETE("posts/draft/{draftId}")
Call<ApiResponse<Void>> deleteDraft(@Path("draftId") String draftId);
```

### 3. 👁️ 帖子详情查看链路
**涉及文件：** `PostDetailActivity.java`, `PostInteractionManager.java`, `PostRepository.java`

-当前链路交互逻辑：用户点击PostAdapter中的帖子 → Intent传递Post对象 → PostDetailActivity接收并渲染标题/内容/图片 → 支持点赞/收藏（立即更新本地UI，异步同步后端） → 支持评论（BottomSheet Inline输入框） → 点击评论按钮跳转CommentsActivity → 内容过长时可展开/收起

-实现细节：
a. ✅ 显示帖子内容（标题/正文/图片）；b. ✅ 点赞功能（即时UI更新 + PostRepository.likePost/unlikePost同步后端）；c. ✅ 收藏功能（即时UI更新 + PostRepository.bookmarkPost/unbookmarkPost同步后端）；d. ✅ 评论功能（Inline BottomSheet输入框，可发送评论）；e. ✅ 内容展开/收起（超过3行显示Expand按钮）；f. ✅ 图片显示（使用GlideUtils.fitCenter加载）

-代码位置：
```33:324:Aura/app/src/main/java/com/aura/starter/PostDetailActivity.java
PostDetailActivity - 详情页实现
```

-可能依赖或关联的链路编号：4.1（点赞收藏）、5.1（评论）

-链路和后端集成，给出后端交互API接口建议（开发2.0阶段考虑）
```java
@GET("posts/{id}")
Call<ApiResponse<PostDetailResponse>> getPostDetail(
    @Path("id") String postId
);
```

### 4. 💬 帖子互动链路（点赞、收藏、评论）
**涉及文件：** `PostDetailActivity.java`, `PostAdapter.java`, `PostInteractionManager.java`, `FeedViewModel.java`, `PostRepository.java`

-当前链路交互逻辑：用户点击点赞/收藏按钮 → 立即更新UI（图标切换filled/outline） → 更新PostInteractionManager本地缓存 → 异步调用PostRepository.likePost/bookmarkPost同步后端 → 失败时回滚UI状态并Toast错误信息

-实现细节：
a. ✅ 点赞功能（PostAdapter/PostDetailActivity都支持，统一使用vm.toggleLike()）；b. ✅ 收藏功能（统一使用vm.toggleBookmark()）；c. ✅ 本地缓存（PostInteractionManager.isLiked/isBookmarked）；d. ✅ 即时反馈（UI立即更新，不等待后端响应）；e. ✅ 错误处理（失败时恢复UI状态）；f. ⚠️ 评论功能（仅支持发送，CommentsAdapter目前是简单实现）

-代码位置：
```61:88:Aura/app/src/main/java/com/aura/starter/PostAdapter.java
PostAdapter - 列表中的互动
```
```81:137:Aura/app/src/main/java/com/aura/starter/PostDetailActivity.java
PostDetailActivity - 详情页中的互动
```

-可能依赖或关联的链路编号：1.1（列表展示）、3.1（详情查看）

-链路和后端集成，给出后端交互API接口建议（开发2.0阶段考虑）
```java
@POST("posts/{id}/like")
Call<ApiResponse<Void>> toggleLike(@Path("id") String postId);
```

### 5. 🔖 帖子书签管理链路
**涉及文件：** `BookmarksFragment.java`, `PostAdapter.java`, `PostRepository.java`

-当前链路交互逻辑：ProfileFragment的第二个Tab → 显示BookmarksFragment → 调用PostRepository.listBookmarkedPosts()获取后端数据 → 转换为Post模型列表 → PostAdapter渲染 → 支持下拉刷新 → 支持上拉加载更多（每页20条） → 支持排序（按时间/点赞数） → 点击可进入详情页/点赞/收藏

-实现细节：
a. ✅ 加载书签（PostRepository.listBookmarkedPosts(20, cursor)）；b. ✅ 下拉刷新（重置cursor为null重新加载）；c. ✅ 上拉加载更多（cursor-based分页）；d. ✅ 排序功能（按时间/点赞数排序）；e. ✅ 互动功能（支持点赞/收藏/查看详情）

-代码位置：
```32:267:Aura/app/src/main/java/com/aura/starter/BookmarksFragment.java
BookmarksFragment - 书签列表实现
```

-可能依赖或关联的链路编号：1.1（帖子展示）、4.1（互动操作）、1.2（个人中心）

-链路和后端集成，给出后端交互API接口建议（开发2.0阶段考虑）
```java
@GET("users/me/bookmarks")
Call<ApiResponse<PostListResponse>> getBookmarkedPosts(
    @Query("page") Integer page,
    @Query("size") Integer size
);
```

### 6. 📝 我的帖子管理链路
**涉及文件：** `MyPostsFragment.java`, `PostAdapter.java`, `PostRepository.java`

-当前链路交互逻辑：ProfileFragment的第一个Tab → 显示MyPostsFragment → 从AuthManager获取当前用户ID → 调用PostRepository.listMyPosts()获取后端数据 → 转换为Post模型列表 → PostAdapter渲染 → 支持排序（按时间/点赞数） → 点击可进入详情页/点赞/收藏

-实现细节：
a. ✅ 加载我的帖子（PostRepository.listMyPosts(20, cursor)）；b. ✅ 获取用户ID（AuthManager.getUserId()）；c. ✅ 排序功能（按时间/点赞数排序）；d. ✅ 互动功能（支持点赞/收藏/查看详情）；e. ⚠️ 暂不支持编辑/删除（需要后端API支持）

-代码位置：
```28:155:Aura/app/src/main/java/com/aura/starter/MyPostsFragment.java
MyPostsFragment - 我的帖子列表实现
```

-可能依赖或关联的链路编号：2.1（创建帖子）、1.1（帖子展示）、1.2（个人中心）

-链路和后端集成，给出后端交互API接口建议（开发2.0阶段考虑）
```java
@GET("users/me/posts")
Call<ApiResponse<PostListResponse>> getMyPosts(
    @Query("page") Integer page,
    @Query("size") Integer size,
    @Query("status") String status  // "published", "draft", "all"
);
```

### 7. 🔍 帖子搜索链路
**涉及文件：** `SearchFragment.java`, `SearchActivity.java`, `SearchResultsActivity.java`, `PostRepository.java`, `PostAdapter.java`

-当前链路交互逻辑：FeedFragment搜索栏 → 跳转SearchActivity → SearchFragment显示 → 5个热门搜索词（fitness/diet/plan/recipe/outcome）水平布局 → 最近搜索历史（最多5条，垂直布局，可删除单条或清空） → 输入搜索词 → 点击搜索或热门词 → 跳转SearchResultsActivity → 调用PostRepository.searchPosts()获取后端数据 → PostAdapter渲染结果列表 → 支持上拉加载更多（无下拉刷新） → 支持点击查看详情/点赞/收藏

-实现细节：
a. ✅ 热门搜索词（5个预定义标签，绿色Chip样式，水平滚动）；b. ✅ 搜索历史（SharedPreferences存储，最多5条，显示最新在顶部）；c. ✅ 搜索功能（PostRepository.searchPosts(query, sort, limit, cursor)）；d. ✅ 结果展示（StaggeredGridLayout 2列布局）；e. ✅ 分页加载（上拉加载更多，无下拉刷新）；f. ✅ 互动功能（支持点赞/收藏/查看详情）

-代码位置：
```33:319:Aura/app/src/main/java/com/aura/starter/SearchFragment.java
SearchFragment - 搜索入口界面
```
```29:286:Aura/app/src/main/java/com/aura/starter/SearchResultsActivity.java
SearchResultsActivity - 搜索结果展示
```

-可能依赖或关联的链路编号：1.1（帖子展示）、3.1（详情查看）、4.1（互动操作）

-链路和后端集成，给出后端交互API接口建议（开发2.0阶段考虑）
```java
@GET("posts/search")
Call<ApiResponse<PostListResponse>> searchPosts(
    @Query("q") String query,
    @Query("page") Integer page,
    @Query("size") Integer size,
    @Query("sort") String sort,  // "relevance", "latest", "popular"
    @Query("filter") String filter  // "all", "title", "content", "tags"
);
```

---


## 👤 个人中心业务链路分析

### 1. 👤 个人资料查看链路
**涉及文件：** `ProfileFragment.java`, `ProfilePagerAdapter.java`, `UserProfile.java`, `UserRepository.java`, `ProfileRepository.java`

-当前链路交互逻辑：用户进入Profile Tab → ProfileFragment加载 → 调用UserRepository.getMyProfile()获取后端数据 → 同时调用UserRepository.getMyStatistics()获取统计数据 → 更新ProfileRepository本地缓存 → UI通过LiveData观察自动更新 → 显示头像/昵称/个人简介 → 显示统计数据（加入天数/餐食数/健康天数/帖子数） → 支持4种主题切换 → ViewPager2显示两个Tab（MyPostsFragment/BookmarksFragment）

-实现细节：
a. ✅ 资料加载（UserRepository.getMyProfile()异步获取）；b. ✅ 统计数据（UserRepository.getMyStatistics()获取4项统计）；c. ✅ 头像显示（Glide加载，支持URI/URL）；d. ✅ 封面显示（Glide加载，支持自定义封面）；e. ✅ 主题切换（4种背景色：橙色/蓝色/紫色/青色）；f. ✅ Tab切换（ProfilePagerAdapter管理MyPostsFragment和BookmarksFragment）；g. ✅ 社交链接（Instagram/Xiaohongshu点击跳转外部链接）

-代码位置：
```34:264:Aura/app/src/main/java/com/aura/starter/ProfileFragment.java
ProfileFragment - 个人中心实现
```
```10:24:Aura/app/src/main/java/com/aura/starter/ProfilePagerAdapter.java
ProfilePagerAdapter - Tab适配器
```

-可能依赖或关联的链路编号：6.1（我的帖子）、5.1（书签列表）、2.1（资料编辑）

-链路和后端集成，给出后端交互API接口建议（开发2.0阶段考虑）
```java
@GET("users/me/profile")
Call<ApiResponse<UserProfileResponse>> getMyProfile();
```

### 2. ✏️ 个人资料编辑链路
**涉及文件：** `ProfileFragment.java`, `UserProfile.java`, `ProfileRepository.java`, `UserRepository.java`

-当前链路交互逻辑：用户点击编辑按钮 → AlertDialog弹窗显示编辑表单 → 编辑昵称/简介/Instagram链接/小红书链接 → 点击Save → ProfileRepository更新本地缓存 → UI自动刷新显示（通过LiveData观察） → 点击头像/封面可替换图片（ActivityResultContracts.GetContent选择相册图片） → 编辑后的头像/封面保存到本地ProfileRepository

-实现细节：
a. ✅ 资料编辑（AlertDialog内嵌表单，更新昵称/简介/社交链接）；b. ✅ 头像上传（点击头像触发图片选择器，保存到ProfileRepository）；c. ✅ 封面上传（点击封面触发图片选择器）；d. ✅ 主题切换（点击4个主题色块更新背景）；e. ✅ 登出功能（长按编辑按钮显示登出确认对话框，清空AuthManager并跳转LoginActivity）；f. ⚠️ 后端同步（目前仅保存到本地，未调用后端API更新）

-代码位置：
```159:183:Aura/app/src/main/java/com/aura/starter/ProfileFragment.java
ProfileFragment - 资料编辑弹窗
```
```103:113:Aura/app/src/main/java/com/aura/starter/ProfileFragment.java
ProfileFragment - 主题切换
```

-可能依赖或关联的链路编号：1.1（资料查看）、1.2（个人中心）

-链路和后端集成，给出后端交互API接口建议（开发2.0阶段考虑）
```java
@PATCH("users/me/profile")
Call<ApiResponse<UserProfileResponse>> updateProfile(
    @Body UserProfileUpdateRequest request
);


---

## 🔗 模块间交互链路分析

基于项目整体架构，以下是帖子和个人中心模块与其他模块（record、run等）之间的交互链路，按相关性从高到低排列：

### 1. 📱 帖子与个人中心交互链路
**相关性：极高** - 用户在浏览帖子时可能需要查看发帖人的个人资料

**涉及文件：** `FeedFragment.java`, `PostDetailActivity.java`, `ProfileFragment.java`

**当前链路交互逻辑：**
```
帖子列表 → 点击头像 → 跳转到用户个人中心 → 显示用户资料和帖子
```

**期望最终链路交互逻辑（复制当前的，等我改即可）：**
```
帖子列表 → 点击头像 → 跳转到用户个人中心 → 显示用户资料和帖子
```

**根据现代化相关模块的前端设计（上网搜）给出优化建议：**
- 添加用户信息悬浮卡片：点击头像时显示用户基本信息，无需跳转
- 社交功能增强：关注/取消关注按钮，私信入口
- 个性化推荐：基于互动历史推荐相关帖子

**可能依赖或关联的链路编号：** 1.1, 2.1, 3.1

**链路和后端集成，给出后端交互API接口建议（开发2.0阶段考虑）：**
```java
// 获取用户信息（轻量级）
@GET("users/{userId}/brief")
Call<ApiResponse<UserBriefDto>> getUserBrief(@Path("userId") String userId);

// 关注用户
@POST("users/{userId}/follow")
Call<ApiResponse<Void>> followUser(@Path("userId") String userId);

// 取消关注
@DELETE("users/{userId}/follow")
Call<ApiResponse<Void>> unfollowUser(@Path("userId") String userId);

// 获取关注状态
@GET("users/{userId}/follow-status")
Call<ApiResponse<FollowStatusDto>> getFollowStatus(@Path("userId") String userId);
```

### 2. 💬 帖子评论与个人中心交互链路
**相关性：高** - 评论作者可能需要查看个人资料

**涉及文件：** `PostDetailActivity.java`, `CommentsAdapter.java`, `ProfileFragment.java`

**当前链路交互逻辑：**
```
帖子详情 → 评论列表 → 点击评论头像 → 跳转到评论者个人中心
```

**期望最终链路交互逻辑（复制当前的，等我改即可）：**
```
帖子详情 → 评论列表 → 点击评论头像 → 跳转到评论者个人中心
```

**根据现代化相关模块的前端设计（上网搜）给出优化建议：**
- 评论区优化：支持表情回复，@提及功能，评论点赞
- 实时互动：评论实时更新，通知提醒
- 社交增强：评论作者标签显示，热门评论置顶

**可能依赖或关联的链路编号：** 3.1, 3.2, 4.3

**链路和后端集成，给出后端交互API接口建议（开发2.0阶段考虑）：**
```java
// 评论点赞
@POST("posts/{postId}/comments/{commentId}/like")
Call<ApiResponse<Void>> likeComment(
    @Path("postId") String postId,
    @Path("commentId") String commentId
);

// 获取评论详情（含互动状态）
@GET("posts/{postId}/comments/{commentId}")
Call<ApiResponse<CommentDetailDto>> getCommentDetail(
    @Path("postId") String postId,
    @Path("commentId") String commentId
);
```

### 3. 📝 帖子创建与个人中心交互链路
**相关性：高** - 创建帖子时需要用户信息和头像

**涉及文件：** `CreateFragment.java`, `ProfileFragment.java`, `UserProfile.java`

**当前链路交互逻辑：**
```
创建帖子 → 显示用户头像和昵称 → 发布成功后刷新个人中心数据
```

**期望最终链路交互逻辑（复制当前的，等我改即可）：**
```
创建帖子 → 显示用户头像和昵称 → 发布成功后刷新个人中心数据
```

**根据现代化相关模块的前端设计（上网搜）给出优化建议：**
- 草稿功能：自动保存草稿，支持单图上传，应用重启后恢复编辑状态
- 发布选项：即时发布，公开范围设置（目前为公开），话题标签选择
- 互动预览：发布前预览效果，支持图片预览和标签显示

**可能依赖或关联的链路编号：** 2.1（草稿保存）、6.1（我的帖子管理）、1.1（帖子列表展示）

**链路和后端集成，给出后端交互API接口建议（开发2.0阶段考虑）：**
```java
// 保存草稿
@POST("posts/draft")
Call<ApiResponse<PostDraftDto>> saveDraft(@Body PostDraftRequest request);

// 获取草稿列表
@GET("users/me/drafts")
Call<ApiResponse<PostDraftListResponse>> getDrafts();

// 删除草稿
@DELETE("posts/draft/{draftId}")
Call<ApiResponse<Void>> deleteDraft(@Path("draftId") String draftId);
```



### 8. 🔍 搜索与帖子交互链路
**相关性：高** - 搜索功能的核心目的是查找帖子内容

**涉及文件：** `SearchFragment.java`, `SearchResultsActivity.java`, `FeedFragment.java`, `PostAdapter.java`

**当前链路交互逻辑：**
```
搜索入口 → 输入搜索词 → 搜索结果页面 → 显示匹配的帖子 → 支持帖子互动操作（支持上拉加载更多，无下拉刷新）
```

**期望最终链路交互逻辑（复制当前的，等我改即可）：**
```
搜索入口 → 输入搜索词 → 搜索结果页面 → 显示匹配的帖子 → 支持帖子互动操作（支持上拉加载更多，无下拉刷新）
```

**根据现代化相关模块的前端设计（上网搜）给出优化建议：**
- 智能搜索：搜索词高亮显示，搜索建议，拼音搜索
- 搜索结果增强：搜索结果预览，相关推荐，搜索热度排行
- 搜索体验优化：搜索历史智能排序，搜索快捷入口，语音搜索

**可能依赖或关联的链路编号：** 1.1, 3.1, 7.1

**链路和后端集成，给出后端交互API接口建议（开发2.0阶段考虑）：**
```java
// 获取搜索建议
@GET("search/suggestions")
Call<ApiResponse<SearchSuggestionsResponse>> getSearchSuggestions(
    @Query("q") String query,
    @Query("limit") Integer limit
);

// 获取热门搜索词
@GET("search/hot-trends")
Call<ApiResponse<HotTrendsResponse>> getHotTrends();

// 记录搜索历史
@POST("search/history")
Call<ApiResponse<Void>> recordSearchHistory(@Body SearchHistoryRequest request);
```

### 9. 📊 统计数据与个人中心交互链路
**相关性：低中** - 个人中心可能展示综合统计数据

**涉及文件：** `ProfileFragment.java`, `RecordFragment.java`, `RunHubActivity.java`

**当前链路交互逻辑：**
```
个人中心 → 查看统计数据 → 从各个模块获取数据 → 汇总展示
```

**期望最终链路交互逻辑（复制当前的，等我改即可）：**
```
个人中心 → 查看统计数据 → 从各个模块获取数据 → 汇总展示
```

**根据现代化相关模块的前端设计（上网搜）给出优化建议：**
- 数据可视化：图表展示各项数据趋势
- 目标设定：健康目标设定和进度跟踪
- 社交对比：与好友数据对比，排名展示

**可能依赖或关联的链路编号：** 1.2, 6.4

**链路和后端集成，给出后端交互API接口建议（开发2.0阶段考虑）：**
```java
// 获取综合统计数据
@GET("users/me/dashboard")
Call<ApiResponse<UserDashboardResponse>> getUserDashboard();

// 设置健康目标
@POST("users/me/goals")
Call<ApiResponse<Void>> setHealthGoals(@Body HealthGoalsRequest request);
---

---

**文档更新日期：** 2025-10-19
**版本：** v1.0
