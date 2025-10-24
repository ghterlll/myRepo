# 后端Api统一说明

by Zhenhao Chen

2025/10/4    17 : 30     请注意文件的时效性



## 1. 通用约定

- **认证**：除测试接口外都需要
   `Authorization: Bearer <ACCESS_TOKEN>`
- **Header**：`Content-Type: application/json`
- **时间格式**：日期 `yyyy-MM-dd`；时间 `yyyy-MM-dd HH:mm:ss`
- **分页游标**：`cursor="createdAt|id"`（倒序列表）；评论子回复用正序时也同名（实现已处理）
- **统一返回**：`ResponseResult<T> { code, message, data }`
   **成功**：`{ "code":0, "message":"SUCCESS", "data": ... }`

## 2. 错误码/提示：

- | Code | Value                    | Description / Notes |
  | ---- | ------------------------ | ------------------- |
  | 1000 | EMAIL CODE INVALID       | 邮箱验证码无效      |
  | 1001 | VERIFICATION CODE ERROR  | 验证码错误          |
  | 1100 | TOKEN INVALID            | Token 无效          |
  | 1101 | TOKEN EXPIRED            | Token 过期          |
  | 1102 | UNAUTHORIZED             | 未授权              |
  | 1110 | REFRESH TOKEN INVALID    | 刷新 Token 无效     |
  | 1200 | USER IS NOT EXISTS       | 用户不存在          |
  | 1201 | BAD CREDENTIALS          | 用户名或密码错误    |
  | 1202 | PHONE ALREADY REGISTERED | 手机号已注册        |
  | 1203 | ACCOUNT NOT VERIFIED     | 账号未验证          |
  | 1204 | ACCOUNT DEACTIVATED      | 账号已停用          |
  | 1300 | POST NOT FOUND           | 帖子未找到          |
  | 1301 | CANNOT FOLLOW SELF       | 不能关注自己        |
  | 1302 | ALREADY FOLLOWING        | 已关注              |
  | 1303 | NOT FOLLOWING            | 未关注              |
  | 1304 | CANNOT BLOCK SELF        | 不能拉黑自己        |
  | 1305 | USER BLOCKED             | 用户已被拉黑        |
  | 1306 | YOU ARE BLOCKED          | 你被对方拉黑        |
  | 1307 | RELATION PRIVATE         | 关系私密            |
  | 1308 | BLOCKED                  | 屏蔽                |
  | 1309 | COMMENT NOT FOUND        | 评论未找到          |
  | 1311 | ALREADY LIKED            | 已点赞              |
  | 1312 | NOT LIKED                | 未点赞              |
  | 1313 | ALREADY BOOKMARKED       | 已收藏              |
  | 1314 | NOT BOOKMARKED           | 未收藏              |
  | 1315 | TITLE REQUIRED           | 标题必填            |
  | 1400 | INVALID PARAM            | 参数无效            |
  | 1403 | FORBIDDEN                | 禁止访问            |
  | 1404 | NOT FOUND                | 资源未找到          |
  | 1500 | NOTIFICATION NOT FOUND   | 通知未找到          |
  | 1600 | FOOD NOT FOUND           | 食物未找到          |
  | 1601 | MEAL LOG NOT FOUND       | 餐食记录未找到      |
  | 1602 | INVALID MEAL TYPE        | 餐食类型无效        |
  | 1700 | EXERCISE NOT FOUND       | 运动记录未找到      |
  | 1701 | EXERCISE LOG NOT FOUND   | 运动日志未找到      |
  | 1800 | OLD PASSWORD WRONG       | 旧密码错误          |
  | 1801 | PASSWORD TOO WEAK        | 新密码太弱          |
  | 1802 | NEW PASSWORD SAME AS OLD | 新旧密码相同        |
  | 1900 | EMAIL NOT MATCH          | 邮箱不匹配          |
  | 1901 | EMAIL NOT BOUND          | 邮箱未绑定          |
  | 1902 | EMAIL CODE USED          | 邮箱验证码已使用    |
  | 1903 | EMAIL CODE EXPIRED       | 邮箱验证码过期      |
  | 1    | success                  | 成功                |
  | 0    | fail                     | 失败                |

------

## 3. 业务魔法值枚举

### 3.1 用户/性别/账号

- `UserStatus`：`0 UNVERIFIED` / `1 ACTIVE` / `2 DEACTIVATED`
- `GenderEnum`：
  - code ↔ text：`0 "female"`, `1 "male"`, `2 "other"`
  - 入参既可传 `gender:"male|female|other"` 也可传 `genderCode:0|1|2`（二选一）

### 3.2 帖子

- `PostVisibility`：`"public"` / `"draft"`
- 点赞收藏幂等：
  - `ALREADY_LIKED(1311)` / `NOT_LIKED(1312)`
  - `ALREADY_BOOKMARKED(1313)` / `NOT_BOOKMARKED(1314)`
- 其他：`POST_NOT_FOUND(1300)`、`FORBIDDEN(1403)`、`TITLE_REQUIRED(1315)`

### 3.3 关注/拉黑

- `CANNOT_FOLLOW_SELF(1301)`、`ALREADY_FOLLOWING(1302)`、`NOT_FOLLOWING(1303)`
- `CANNOT_BLOCK_SELF(1304)`、`USER_BLOCKED(1305)`、`YOU_ARE_BLOCKED(1306)`
- 关系隐私：`RELATION_PRIVATE(1307)`；互黑访问：`BLOCKED_ACCESS(1308)`

### 3.4 评论

- `COMMENT_NOT_FOUND(1309)`、`INVALID_PARAM(1400)`

### 3.5 饮食

- `MealType`：`0 早餐` / `1 午餐` / `2 晚餐` / `3 加餐`
- `DietRule` 健康日阈值：`LOWER=0.8`、`UPPER=1.1`（服务端内用）

### 3.6 通知

- `NoticePriority`：`0 NORMAL` / `1 IMPORTANT` / `2 URGENT`

### 3.7 邮箱验证码/密码

- `EMAIL_NOT_BOUND(1901)` / `EMAIL_MISMATCH(1900)`
- `EMAIL_CODE_INVALID(1000)` / `EMAIL_CODE_USED(1902)` / `EMAIL_CODE_EXPIRED(1903)`
- `OLD_PASSWORD_WRONG(1800)` / `PASSWORD_WEAK(1801)` / `NEW_PASSWORD_SAME(1802)`
- 验证码用途（服务端内用）：`EmailCodePurpose.RESET_PASSWOR`

## 4. API接口

### 4.1 认证 & 账号

#### 注册 / 登录 / 刷新

**POST /users/register**
 req = `UserDtos.RegisterReq`

```
{
  "phone": "13800000002",
  "password": "123456",
  "nickname": "tester"
}
```

resp

```
{ "code": 0, "message": "SUCCESS" }
```

**POST /auth/login**
 req = `UserDtos.LoginReq`

```
{ "phone":"13800000002", "password":"123456", "deviceId":"web" }
```

resp (data = `UserDtos.TokenPair`)

```
{
  "code":0,
  "message":"SUCCESS",
  "data":{"accessToken":"...", "refreshToken":"..."}
}
```

**POST /auth/refresh**
 req = `RefreshReq`

```
{ "refreshToken":"...", "deviceId":"web" }
```

resp 同上

**POST /users/me/deactivate** 账号注销
 resp：`{ "code":0, "message":"SUCCESS" }`

#### 重置密码（邮箱验证码）

**POST /users/me/password/reset/code**
 req = `SendResetCodeReq`（必须与绑定邮箱一致）

```
{ "email": "plzdonotreply@qq.com" }
```

resp 成功无数据

**POST /users/me/password**
 req = `ResetPasswordReq`

```
{
  "oldPassword": "123456",
  "newPassword": "abc12345",
  "email": "plzdonotreply@qq.com",
  "code": "734128"
}
```

resp 成功无数据
 错误示例（未点赞取消点赞等同理）：
 `{ "code": 1904, "message": "OLD_PASSWORD_WRONG" }`

------

### 4.2 个人资料 / 我的聚合

**GET /users/me**
 resp data = `UserAggregateResp`

**PATCH /users/me/profile**
 req = `UserProfileUpdateReq`（部分字段可选，传则更新）

```
{
  "avatarUrl": "https://xx/av.png",
  "nickname": "newNick",
  "gender": "male",
  "birthday": "1994-05-20",
  "heightCm": 178,
  "latestWeightKg": 72.5,
  "targetWeightKg": 68.0,
  "targetDeadline": "2025-12-31",
  "activityLvl": 1,
  "age": 30,
  "location": "Shanghai",
  "deviceType": "iOS",
  "interests": "跑步, 健身"
}
```

**GET /me/statistics**
 resp data：

```
{ "joinedDays": 128, "mealCount": 356, "healthyDays": 92 }
```

------

### 4.3 运动

**POST /exercises** （新增一条运动）
 req = `ExerciseAddReq`

```
{
  "sourceType": 0,
  "sourceId": 1001,
  "name": null,
  "minutes": 45,
  "date": "2025-10-03"
}
```

resp：`{ "data": { "id": 123 } }`

**DELETE /exercises/{id}** 删除

**POST /exercises/custom-items** （创建自定义运动项）
 req = `ExerciseItemCreateReq`

```
{ "name": "HIIT", "kcalPerMin": 12 }
```

resp：`{ "data": { "id": 88 } }`

**GET /exercises/day?date=2025-10-03**
 resp data = `DailyWorkoutResp`

```
{
  "date":"2025-10-03",
  "totalKcal": 520,
  "items":[
    {
      "id":101, "name":"跑步", "minutes":30, "kcal":300,
      "sourceType":0, "sourceId":2001, "createdAt":"2025-10-03 09:20:00"
    }
  ]
}
```

------

### 4.4 饮食 & 当日小结

**POST /meals**
 req = `MealAddReq`

```
{
  "mealType": 0,
  "sourceType": 0,
  "sourceId": 666,
  "itemName": null,
  "unitName": null,
  "unitQty": 1.0,
  "date": "2025-10-03"
}
```

（自由录入示例：`"sourceType":2, "itemName":"水煮蛋", "unitName":"1个", "unitQty":2`）

**DELETE /meals/{id}** 删除

**GET /meals/day?date=2025-10-03**
 resp data = `DailySummaryResp`

```
{
  "date":"2025-10-03",
  "totalKcal": 1870,
  "carbsG": 210.5, "proteinG": 95.0, "fatG": 60.5,
  "items":[
    {
      "id":11, "mealType":0, "itemName":"燕麦",
      "unit":"100克", "qty":1, "kcal": 380,
      "carbsG":65, "proteinG":12, "fatG":7,
      "createdAt":"2025-10-03 08:10:00"
    }
  ]
}
```

**GET /me/day?date=2025-10-03**（日总览，吃+动）
 resp data = `DayOverviewResp`

```
{
  "date":"2025-10-03",
  "intakeKcal": 1870,
  "burnKcal": 520,
  "remainingKcal": 350,
  "one":   { "consumedG":210.5, "targetG":260.0 },
  "two":   { "consumedG":95.0,  "targetG":120.0 },
  "three": { "consumedG":60.5,  "targetG":70.0 }
}
```

------

### 4.5 通知中心

**GET /notices?limit=20&cursor=2025-10-03 10:00:00|123**
 resp data：`{ "items": NoticeCardResp[] }`

**GET /notices/{id}** → `NoticeDetailResp`
 **POST /notices/{id}/read**、**POST /notices/read_all**、**GET /notices/unread_count**

（管理）**POST /admin/notices**
 req = `NoticeCreateReq`

```
{
  "title":"系统升级",
  "content":"今晚2点维护",
  "linkUrl":"https://...",
  "priority":1,
  "scheduledAt":"2025-10-04 02:00:00",
  "status":1
}
```

------

### 4.6 社交关系：关注 / 拉黑 / 隐私

**POST /users/{id}/follow**，**DELETE /users/{id}/follow**
 **POST /users/{id}/block**，**DELETE /users/{id}/block**

**GET /me/followers** / **GET /me/followings**
 **GET /users/{id}/followers** / **GET /users/{id}/followings**
 resp：`{ "items": [ <userId>, ... ] }`（用户卡片请前端批量拉用户服务）

**PATCH /me/privacy/relations**
 req = `RelationPrivacyUpdateReq`

```
{ "followersVisible": true, "followingsVisible": false }
```

**GET /users/{id}/relation**
 resp

```
{ "following": true, "blocked": false, "blockedBy": false }
```

> 规则重申：互黑 → 互相不可见、不可关注；拉黑会解除双方关注。隐私关闭 → 列表接口伪装不存在。

------

### 4.7 帖子 & 标签 & 互动

#### 帖子 CRUD / 详情 / 列表

**POST /posts**
 req = `PostCreateReq`

```
{
  "title":"今天在中央公园跑步",
  "caption":"#CentralPark 天气不错～",
  "publish": true,
  "tags": ["centralpark", "running"],
  "medias":[{"url":"https://img/1.jpg","width":1080,"height":1440,"sortOrder":0}]
}
```

resp：`{ "data": { "id": 123 } }`

**PATCH /posts/{postId}**
 req = `PostUpdateReq`

```
{ "title":"标题改一下", "caption":"文案改一下", "tags":["coffeeTime"] }
```

**PUT /posts/{postId}/media**
 req = `MediaItem[]`

**POST /posts/{postId}/publish** / **POST /posts/{postId}/hide**
 **DELETE /posts/{postId}** 软删

**GET /posts/{postId}**
 resp data = `PostDetailResp`

```
{
  "id":123, "authorId":1001,
  "title":"今天在中央公园跑步",
  "caption":"#CentralPark 天气不错～",
  "status":"public",
  "tags":["centralpark","running"],
  "medias":[{"url":"https://img/1.jpg","width":1080,"height":1440,"sortOrder":0}],
  "createdAt":"2025-10-03 10:00:00",
  "updatedAt":"2025-10-03 10:00:00"
}
```

**GET /posts?limit=20&cursor=2025-10-03 10:00:00|123**（公共流）
 **GET /posts/feed/followings?limit=20&cursor=...**（关注流）
 resp data：`{ "items": PostCardResp[] }`

```
{
  "items":[
    { "id":123, "coverUrl":"https://img/1.jpg", "authorId":1001, "title":"...", "createdAt":"2025-10-03 10:00:00" }
  ]
}
```

> 已实现哈希话题解析：正文里的 `#CentralPark` 会被解析为标签写入（`name_lc` 小写去重，展示名保留首次风格；中文不变）。



#### 点赞 / 收藏（强幂等）

**POST /posts/{postId}/like** → 已点赞会报 `ALREADY_LIKED`
 **DELETE /posts/{postId}/like** → 未点赞会报 `NOT_LIKED`
 **POST /posts/{postId}/bookmark** → 已收藏会报 `ALREADY_BOOKMARKED`
 **DELETE /posts/{postId}/bookmark** → 未收藏会报 `NOT_BOOKMARKED`
 resp 成功均无数据（计数实时+/-，不会为负）



#### 评论（楼中楼）

**POST /posts/{postId}/comments**
 req = `CommentCreateReq`

```
{ "content":"写得真棒！", "parentId": null }
```

（回复某条评论）

```
{ "content":"同意你的看法", "parentId": 56789 }
```

resp：`{ "data": { "id": 67890 } }`

**DELETE /comments/{commentId}**（作者本人或帖主可删）

**GET /posts/{postId}/comments?limit=20&cursor=2025-10-03 11:00:00|555&preview=3**
 resp data = `[{ root: CommentResp, replies: CommentResp[] }]`

**GET /comments/{rootId}/replies?limit=20&cursor=2025-10-03 11:05:00|777**
 resp data = `CommentResp[]`（正序）



#### 标签管理 / 绑定

**POST /tags**  创建
 req = `TagCreateReq`

```
{ "name": "CentralPark" }
```

**PATCH /tags/{id}** 修改展示名
 **DELETE /tags/{id}** 删除
 **GET /tags?q=cen&limit=20&offset=0** → `TagResp[]`

**PUT /posts/{postId}/tags**（替换这篇帖子的所有标签）
 req = `PostTagReplaceReq`（可传 `names` 或 `tagIds`，都传以 `names` 为准）

```
{ "names": ["centralpark","running"] }
```

**GET /posts/{postId}/tags** → `TagResp[]`

**GET /tags/{tagId}/posts?limit=20&cursor=...**（标签页帖子流，公共+非互黑）
 resp：`{ "items": PostCardResp[] }`

------

### 4.8 曝光埋点

**POST /exposures**
 req = `ExposureCreateReq`

```
{ "contentId":"post:123", "platform":"ios", "device":"iPhone14,3", "city":"Shanghai" }
```

------

### 4.9 测试

**GET /api/test/hello** → `"hello"`
 **GET /api/test/ping** → `{ "ok": true, "msg":"pong" }` 



------

## 变更摘要（2025/10/4）

1. 新增 **4.10 体重**、**4.11 喝水** 两节 API。
2. **4.2 更新画像**：补充 `initialWeightKg/initialWeightAt`、`latestWeightAt` 字段说明与示例。
3. 错误码复用现有集合，参数非法一律返回 `1400 INVALID PARAM`。

------

### 4.2 个人资料 / 我的聚合（补充）

**PATCH /users/me/profile**
 req = `UserProfileUpdateReq`（部分可选，传则更新；日期 `yyyy-MM-dd`）

```json
{
  "avatarUrl": "https://xx/av.png",
  "nickname": "newNick",
  "gender": "male",
  "genderCode": 1,
  "birthday": "1994-05-20",
  "heightCm": 178,

  "initialWeightKg": 78.0,
  "initialWeightAt": "2025-01-01",

  "latestWeightKg": 72.5,
  "latestWeightAt": "2025-10-03",

  "targetWeightKg": 68.0,
  "targetDeadline": "2025-12-31",

  "activityLvl": 1,
  "age": 30,
  "location": "Shanghai",
  "deviceType": "iOS",
  "interests": "跑步, 健身"
}
```

> 说明：体重曲线由“体重日志”维护；提交体重后会**自动回写**画像的「初始/最新体重+日期」。

------

### 4.10 体重

> 用于记录与查询体重曲线。提交为**按日去重**（同一天多次提交会被覆盖为最后一次）。

#### 提交 / 更新当天体重（幂等按日）

**POST /weights**
 req = `WeightSubmitReq`

```json
{
  "date": "2025-10-03",       // 可省略=今天
  "weightKg": 72.80,
  "note": "晚饭后称重"
}
```

resp：`{ "code":0, "message":"SUCCESS" }`
 校验：`weightKg > 0`，非法返回 `1400 INVALID PARAM`。
 副作用：自动根据所有记录**重算**画像中的 `initialWeightKg/At` 与 `latestWeightKg/At`。

------

#### 区间查询（折线图）

**GET /weights/range?start=2025-09-01&end=2025-10-03**
 resp data = `WeightRangeResp`

```json
{
  "items": [
    { "date": "2025-09-01", "weightKg": 73.4 },
    { "date": "2025-09-15", "weightKg": 72.9 }
  ]
}
```

> `start/end` 为空会各自默认为“今天”与“今天-29天”。

------

#### 最新/目标/初始汇总

**GET /weights/latest**
 resp data = `WeightLatestResp`

```json
{
  "latestDate":  "2025-10-03",
  "latestWeightKg": 72.5,
  "initialDate": "2025-01-01",
  "initialWeightKg": 78.0,
  "targetWeightKg": 68.0,
  "targetDate": "2025-12-31"
}
```

------

### 4.11 喝水

> 记录每天饮水总量（毫升）。同一天多次提交会**覆盖**为最后一次。

#### 提交/更新某日饮水

**POST /water**
 req = `WaterSubmitReq`

```json
{
  "date": "2025-10-03",   // 可省略=今天
  "amountMl": 1600        // 范围 [0, 100000]
}
```

resp：`{ "code":0, "message":"SUCCESS" }`
 非法范围返回 `1400 INVALID PARAM`。

------

#### 删除某日饮水

**DELETE /water/day?date=2025-10-03**
 resp：`{ "code":0, "message":"SUCCESS" }`

------

#### 单日查询

**GET /water/day?date=2025-10-03**
 resp data = `WaterDayResp`

```json
{ "date": "2025-10-03", "amountMl": 1600 }
```

> `date` 省略=今天。

------

#### 区间查询（柱状/折线）

**GET /water/range?from=2025-09-28&to=2025-10-04**
 resp data = `WaterRangeResp`

```json
{
  "items": [
    { "date": "2025-09-28", "amountMl": 0 },
    { "date": "2025-09-29", "amountMl": 1200 },
    { "date": "2025-09-30", "amountMl": 800 }
  ]
}
```

> `from/to` 省略时默认最近 7 天；服务端会**补齐缺失日期**为 0 ml，便于前端直接绘图。

------

#### 备注 · 体重/喝水对“日总览”的影响

- 体重与喝水目前**不参与** `DayOverviewResp` 的卡路里计算；如需在前端展示，可直接分别调 **4.10** / **4.11**。
- 后续如需要把“喝水目标/达成进度”加入总览，可在 `overviewService.dayOverview(...)` 里聚合 `WaterService.day(...)` 的结果扩展返回。

### 4.12 食物库 & 搜索（系统库 + 自建库）

> 场景：用于选择食物创建餐食记录。系统库 `food_item` + 用户自建库 `user_food_item`。
>  说明：所有列表均为分页；`q` 同时命中 **name / alias**（中文直接 `LIKE`）。

DTO 说明（简化）

- `FoodItemResp`

  ```
  {
    "id": 25,
    "name": "可口可乐",
    "unitName": "330毫升",
    "kcalPerUnit": 138,
    "carbsG": 33.0,
    "proteinG": 0.0,
    "fatG": 0.0,
    "createdAt": "2025-10-03 01:05:32"
  }
  ```

- `FoodCreateReq` / `FoodUpdateReq`

  ```
  {
    "name": "水煮蛋",
    "unitName": "1个",
    "kcalPerUnit": 78,
    "carbsG": 0.6,
    "proteinG": 6.3,
    "fatG": 5.3
  }
  ```

------

#### 4.12.1 合并搜索（系统库 + 自建库）

**GET /foods**
 req params：

- `q` 关键字（可空）
- `scope`: `all`（默认）/`system`/`user`
- `limit`（默认 20，1~100）
- `offset`（默认 0）

示例：

```
GET /foods?q=可乐&scope=all&limit=20&offset=0
Authorization: Bearer <ACCESS_TOKEN>
```

resp：

```
{
  "code": 0,
  "message": "SUCCESS",
  "data": {
    "items": [ /* FoodItemResp[]，来源已折叠，前端以详情接口区分 */ ]
  }
}
```

------

#### 4.12.2 食物详情（系统 or 自建）

**GET /foods/{source}/{id}**
 path：

- `source`: `system` | `user`
- `id`: Long

示例：

```
GET /foods/system/25
GET /foods/user/197544665...
Authorization: Bearer <ACCESS_TOKEN>
```

resp：

```
{ "code": 0, "message": "SUCCESS", "data": { /* FoodItemResp */ } }
```

------

#### 4.12.3 我的自建食物 CRUD

**POST /my/foods**（创建）

```
POST /my/foods
Authorization: Bearer <ACCESS_TOKEN>
Content-Type: application/json

{
  "name": "水煮蛋",
  "unitName": "1个",
  "kcalPerUnit": 78,
  "carbsG": 0.6,
  "proteinG": 6.3,
  "fatG": 5.3
}
```

resp：

```
{ "code":0, "message":"SUCCESS", "data":{ "id": 19754466... } }
```

**PATCH /my/foods/{id}**（更新）

```
PATCH /my/foods/19754466...
Authorization: Bearer <ACCESS_TOKEN>
Content-Type: application/json

{ "name": "溏心蛋", "kcalPerUnit": 82 }
```

resp：`{ "code":0, "message":"SUCCESS" }`

**DELETE /my/foods/{id}**（删除）

```
DELETE /my/foods/19754466...
Authorization: Bearer <ACCESS_TOKEN>
```

resp：`{ "code":0, "message":"SUCCESS" }`

**GET /my/foods**（我的列表）

```
GET /my/foods?limit=20&offset=0
Authorization: Bearer <ACCESS_TOKEN>
```

resp：

```
{ "code":0, "message":"SUCCESS", "data": { "items": [ /* FoodItemResp[] */ ] } }
```

------

#### 4.12.4 系统库列表（仅 `food_item`）

**GET /foods/system**
 req params：

- `q` 关键字（可空，命中 name/alias）
- `category`（可空，如 `drink|staple|vegetable|fruit|snack|soup...`）
- `gi` GI 标签（可空：`low|medium|high`）
- `limit`（默认 20，1~100）
- `offset`（默认 0）

示例：

```
GET /foods/system?q=可乐&category=drink&gi=low&limit=20&offset=0
Authorization: Bearer <ACCESS_TOKEN>
```

resp：

```
{
  "code": 0,
  "message": "SUCCESS",
  "data": { "items": [ /* FoodItemResp[] */ ] }
}
```

------

#### 4.12.5（可选）系统库管理端 CRUD

> 仅管理端开放。普通用户不要调用。

**POST /admin/foods**（创建）
 **PATCH /admin/foods/{id}**（更新）
 **DELETE /admin/foods/{id}**（删除）

请求体同 `FoodCreateReq`/`FoodUpdateReq`，响应与自建库一致。