# Aura 前端 API 需求与数据模型文档

**项目:** Aura Health Companion (Android)  
**版本:** 1.0  
**日期:** 2025-10-14  
**说明:** 本文档描述前端应用所需的所有后端API接口、请求/响应数据结构以及前端数据模型映射

---

## 📱 1. 应用架构说明

### 1.1 技术栈
- **前端框架:** Android (Java)
- **网络库:** Retrofit 2 + OkHttp 3
- **数据存储:** SharedPreferences (仅存储认证信息)
- **后端通信:** RESTful API (JSON)
- **认证方式:** JWT (Access Token + Refresh Token)

### 1.2 网络架构
```
Android App (前端)
    ↓
ApiClient (Retrofit配置)
    ↓
ApiService (接口定义)
    ↓
AuraRepository (数据仓库)
    ↓
Activities/Fragments (UI层)
```

---

## 🔐 2. 认证系统

### 2.1 用户注册
**API:** `POST /users/register`

**前端请求模型:** `RegisterRequest.java`
```java
{
    "phone": String,        // 手机号
    "password": String,     // 密码
    "nickname": String,     // 昵称
    "deviceId": String      // 设备ID
}
```

**后端响应:**
```json
{
    "code": 0,
    "message": "SUCCESS",
    "data": null
}
```

---

### 2.2 用户登录
**API:** `POST /users/login`

**前端请求模型:** `LoginRequest.java`
```java
{
    "phone": String,
    "password": String,
    "deviceId": String
}
```

**前端响应模型:** `TokenResponse.java`
```json
{
    "code": 0,
    "message": "SUCCESS",
    "data": {
        "accessToken": String,    // 访问令牌 (15分钟有效)
        "refreshToken": String,   // 刷新令牌 (7天有效)
        "expiresIn": Integer      // 过期时间(秒)
    }
}
```

**前端存储:** 
- 通过 `AuthManager` 保存到 `SharedPreferences`
- 自动添加到后续所有请求的 Header: `Authorization: Bearer {accessToken}`

---

### 2.3 Token 刷新
**API:** `POST /users/refresh`

**前端请求模型:** `RefreshTokenRequest.java`
```java
{
    "refreshToken": String,
    "deviceId": String
}
```

**响应:** 同 2.2 登录响应

**前端处理:**
- 当收到 `code: 1101` (TOKEN_EXPIRED) 时自动清除登录信息
- 跳转到登录页面

---

## 💧 3. 饮水记录模块

### 3.1 添加饮水记录
**API:** `POST /water`

**前端请求模型:** `WaterAddRequest.java`
```java
{
    "date": String,      // yyyy-MM-dd (可选，默认今天)
    "amountMl": Integer  // 饮水量(毫升) [0-100000]
}
```

**响应:**
```json
{
    "code": 0,
    "message": "SUCCESS",
    "data": null
}
```

**前端使用场景:**
- `WaterRecordSheet.java` - 快速记录饮水
- `WaterIntakeActivity.java` - 饮水记录页面

---

### 3.2 查询每日饮水
**API:** `GET /water/day?date=2025-10-14`

**前端响应模型:** `WaterDailySummaryResponse.java`
```java
{
    "code": 0,
    "message": "SUCCESS",
    "data": {
        "date": String,        // yyyy-MM-dd
        "amountMl": Integer    // 当天总饮水量(毫升)
    }
}
```

**前端字段映射:**
- `amountMl` → `getTotalMl()` (向后兼容方法)

---

### 3.3 查询饮水范围
**API:** `GET /water/range?from=2025-10-01&to=2025-10-14`

**前端响应模型:** `WaterRangeResponse.java`
```java
{
    "code": 0,
    "message": "SUCCESS",
    "data": {
        "items": [
            {
                "date": String,      // yyyy-MM-dd
                "amountMl": Integer  // 当天饮水量
            }
        ]
    }
}
```

---

## ⚖️ 4. 体重记录模块

### 4.1 记录体重
**API:** `POST /weights`

**前端请求模型:** `WeightLogRequest.java`
```java
{
    "date": String,           // yyyy-MM-dd (可选，默认今天)
    "weightKg": BigDecimal,   // 体重(公斤) 必须 > 0
    "note": String            // 备注 (可选)
}
```

**响应:**
```json
{
    "code": 0,
    "message": "SUCCESS",
    "data": null
}
```

**前端使用场景:**
- `WeightTrendActivity.java` - 体重趋势页面

---

### 4.2 获取最新体重信息
**API:** `GET /weights/latest`

**前端响应模型:** `WeightLogResponse.java`
```java
{
    "code": 0,
    "message": "SUCCESS",
    "data": {
        "latestDate": String,         // 最近一次称重日期
        "latestWeightKg": BigDecimal, // 最近体重
        "initialDate": String,        // 初始称重日期
        "initialWeightKg": BigDecimal,// 初始体重
        "targetWeightKg": BigDecimal, // 目标体重
        "targetDate": String          // 目标日期
    }
}
```

**前端使用场景:**
- `RecordFragment.java` - 首页显示当前体重
- `WeightTrendActivity.java` - 显示体重目标和进度

---

### 4.3 查询体重历史
**API:** `GET /weights/range?start=2025-09-01&end=2025-10-14`

**前端响应模型:** `WeightHistoryResponse.java`
```java
{
    "code": 0,
    "message": "SUCCESS",
    "data": {
        "items": [
            {
                "date": String,          // yyyy-MM-dd
                "weightKg": BigDecimal   // 体重
            }
        ]
    }
}
```

**前端字段映射:**
- `items` → `getLogs()` (向后兼容方法)
- 每个 item 类型为 `WeightDayItem`

---

## 🍽️ 5. 饮食记录模块

### 5.1 搜索食物
**API:** `GET /foods?query=chicken&category=meat`

**前端响应模型:** `FoodSearchResponse.java`
```java
{
    "code": 0,
    "message": "SUCCESS",
    "data": {
        "items": [
            {
                "id": Long,
                "name": String,
                "bilableName": String,
                "calories": Integer,
                "protein": Double,
                "fat": Double,
                "carbs": Double,
                "fiber": Double
            }
        ]
    }
}
```

**前端使用场景:**
- `FoodSelectionActivity.java` - 食物选择页面

---

### 5.2 添加餐食记录
**API:** `POST /meal`

**前端请求模型:** `MealAddRequest.java`
```java
{
    "date": String,       // yyyy-MM-dd
    "mealType": Integer,  // 0:早餐 1:午餐 2:晚餐 3:加餐
    "foodItemId": Long,   // 食物ID
    "servings": Double    // 份数
}
```

**响应:**
```json
{
    "code": 0,
    "message": "SUCCESS",
    "data": Long  // 返回餐食记录ID
}
```

---

### 5.3 查询每日餐食汇总
**API:** `GET /meal/daily-summary?date=2025-10-14`

**前端响应模型:** `DailySummaryResponse.java`
```java
{
    "code": 0,
    "message": "SUCCESS",
    "data": {
        "date": String,
        "totalCalories": Integer,
        "totalProtein": Double,
        "totalFat": Double,
        "totalCarbs": Double,
        "meals": [
            {
                "id": Long,
                "mealType": Integer,
                "foodName": String,
                "servings": Double,
                "calories": Integer
            }
        ]
    }
}
```

**前端使用场景:**
- `RecordFragment.java` - 显示每日卡路里和餐食列表

---

### 5.4 删除餐食记录
**API:** `DELETE /meal/{id}`

**响应:**
```json
{
    "code": 0,
    "message": "SUCCESS",
    "data": null
}
```

---

## 👤 6. 用户资料模块

### 6.1 获取个人信息
**API:** `GET /users/me`

**前端响应模型:** `UserProfileResponse.java`
```java
{
    "code": 0,
    "message": "SUCCESS",
    "data": {
        "userId": Long,
        "phone": String,
        "nickname": String,
        "email": String,
        "avatar": String,
        "gender": String,
        "birthday": String,
        "heightCm": Integer,
        "bio": String
    }
}
```

**前端使用场景:**
- `ProfileFragment.java` - 个人资料页面
- `WeightTrendActivity.java` - 获取身高等信息

---

## 🔧 7. 通用数据结构

### 7.1 统一响应格式
**前端模型:** `ApiResponse<T>.java`
```java
public class ApiResponse<T> {
    private int code;       // 0=成功, 其他=错误码
    private String message; // 响应消息
    private T data;         // 响应数据
    
    public boolean isSuccess() {
        return code == 0;  // ⚠️ 成功码是 0
    }
}
```

### 7.2 常见错误码
| Code | 含义 | 前端处理 |
|------|------|----------|
| 0 | SUCCESS | 操作成功 |
| 1100 | TOKEN INVALID | 清除登录，跳转登录页 |
| 1101 | TOKEN EXPIRED | 清除登录，跳转登录页 |
| 1102 | UNAUTHORIZED | 清除登录，跳转登录页 |
| 1200 | USER NOT EXISTS | 显示错误提示 |
| 1201 | BAD CREDENTIALS | 显示"用户名或密码错误" |
| 1400 | INVALID PARAM | 显示"参数错误" |

---

## 🚀 8. 前端网络层实现

### 8.1 核心类

#### ApiClient.java
- 配置 Retrofit 和 OkHttp
- 添加认证拦截器（自动附加 Bearer Token）
- 配置超时时间：连接15秒，读取20秒

#### ApiService.java
- 定义所有 API 接口
- 使用 Retrofit 注解声明 HTTP 方法和路径

#### AuraRepository.java
- 封装所有 API 调用
- 提供统一的错误处理
- 检测 Token 过期并自动跳转登录

#### AuthManager.java
- 管理用户认证信息
- 保存/读取 Token 到 SharedPreferences
- 提供登出功能

---

## 📊 9. 数据流程示例

### 9.1 用户登录流程
```
1. LoginActivity 获取用户输入
   ↓
2. AuraRepository.login(phone, password)
   ↓
3. ApiService.login() 发送 POST 请求
   ↓
4. 后端返回 TokenResponse
   ↓
5. AuthManager 保存 tokens 到 SharedPreferences
   ↓
6. ApiClient 自动在后续请求中添加 Authorization header
   ↓
7. 跳转到 MainActivity
```

### 9.2 添加饮水记录流程
```
1. WaterRecordSheet 用户点击确认
   ↓
2. AuraRepository.addWater(date, amountMl)
   ↓
3. 创建 WaterAddRequest 对象
   ↓
4. ApiService.addWater() 发送 POST 请求
   ↓
5. 检查响应: response.isSuccess()
   ↓
6. 如果成功: 显示成功提示，刷新界面
   ↓
7. 如果失败: 
   - code == 1101? 跳转登录
   - 其他: 显示错误消息
```

---

## ⚠️ 10. 重要注意事项

### 10.1 字段命名规则
- **后端 API:** 使用 `camelCase` (如: `amountMl`, `weightKg`)
- **前端模型:** 必须与后端完全一致
- **数据库表:** 使用 `snake_case` (如: `amount_ml`, `weight_kg`)
- **MyBatis-Plus:** 自动处理 camelCase ↔ snake_case 转换

### 10.2 必须遵守的规则
1. ✅ **成功判断:** 使用 `code == 0` 而不是 `code == 1`
2. ✅ **Token处理:** 检测 `1101` 错误码并跳转登录
3. ✅ **异步操作:** 网络请求必须在后台线程执行
4. ✅ **UI更新:** 使用 `Handler.post()` 在主线程更新UI
5. ✅ **错误处理:** 捕获异常并显示友好提示

### 10.3 当前已知问题
⚠️ **后端数据库字段不匹配问题需要后端团队修复:**

1. **WaterIntake.java** - 定义了 `updatedAt` 字段，但数据库表没有
2. **UserProfile.java** - 定义了多个不存在的字段（age, location等）

**临时解决方案:** 前端无法修复，需要等待后端更新

---

## 📝 11. 前端数据模型清单

### 11.1 请求模型 (Request)
| 类名 | 用途 | 位置 |
|------|------|------|
| RegisterRequest | 注册 | network/models/ |
| LoginRequest | 登录 | network/models/ |
| RefreshTokenRequest | 刷新Token | network/models/ |
| WaterAddRequest | 添加饮水 | network/models/ |
| WeightLogRequest | 记录体重 | network/models/ |
| MealAddRequest | 添加餐食 | network/models/ |

### 11.2 响应模型 (Response)
| 类名 | 用途 | 位置 |
|------|------|------|
| TokenResponse | 登录响应 | network/models/ |
| WaterDailySummaryResponse | 每日饮水 | network/models/ |
| WaterRangeResponse | 饮水范围 | network/models/ |
| WeightLogResponse | 最新体重 | network/models/ |
| WeightHistoryResponse | 体重历史 | network/models/ |
| DailySummaryResponse | 每日餐食 | network/models/ |
| FoodSearchResponse | 食物搜索 | network/models/ |
| UserProfileResponse | 用户资料 | network/models/ |

---

## 🔄 12. 版本更新说明

### Version 1.0 (2025-10-14)
- ✅ 移除本地 Room 数据库
- ✅ 完全迁移到后端 REST API
- ✅ 实现 JWT 认证
- ✅ 添加 Token 过期自动处理
- ✅ 统一错误处理机制
- ⚠️ 发现后端数据库字段不匹配问题

---

## 📞 13. 技术支持

**前端负责人:** [你的名字]  
**后端API文档:** `/Users/qiaoxinying/Desktop/Aura/后端API说明.md`  
**问题反馈:** 请通过团队协作工具报告问题

---

**文档生成时间:** 2025-10-14  
**下次更新:** 待后端修复数据库字段不匹配问题后更新

