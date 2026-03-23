# SY Photos 安卓接口详细文档

本文档基于当前仓库里的实际 PHP 代码整理，目标是给安卓端一个“按现状可直接对接”的接口说明，避免再靠猜。

适用范围：

- 已实现的 App API：`/api/app/v1/...`
- 安卓端可能会用到的辅助接口：`/photo_asset.php`、`/api/GetAirPortName.php`、`/api/plane-info.php`
- 说明当前还没有实现的能力，避免安卓端误接

## 1. 基础约定

### 1.1 Base URL

生产站基础地址来自配置：

- `https://www.syphotos.cn`

App API 推荐完整前缀：

- `https://www.syphotos.cn/api/app/v1`

### 1.2 请求格式

`/api/app/v1/...` 统一支持两种入参方式：

- `Content-Type: application/json`
- 普通表单提交：`application/x-www-form-urlencoded`

说明：

- 如果是 JSON，请求体会被 `json_decode`
- 如果不是 JSON，后端直接读 `$_POST`
- 这些接口目前没有实现 `multipart/form-data` 上传接口

### 1.3 通用响应格式

成功：

```json
{
  "success": true
}
```

失败：

```json
{
  "success": false,
  "error": "错误说明"
}
```

常见 HTTP 状态码：

- `200` 成功
- `201` 创建成功
- `400` 普通参数错误
- `401` 未登录、access token 失效、refresh token 无效
- `403` 账号不可用、邮箱未验证、封禁
- `404` 资源不存在
- `405` 请求方法错误
- `422` 参数缺失、业务校验失败
- `429` 请求频率过高
- `500` 服务端错误

### 1.4 认证方式

受保护接口使用 Bearer Token：

```http
Authorization: Bearer {access_token}
```

Token 规则：

- `access_token` 有效期：`2 小时`
- `refresh_token` 有效期：`30 天`
- 刷新 token 时，旧 session 会被直接作废，然后签发一组新的 access/refresh token

### 1.5 设备信息字段

登录、刷新时，后端会使用这些字段记录设备：

- `device_id`
- `device_name`
- `platform`
- `system_version`
- `app_version`
- `push_token`

默认行为：

- `device_id` 不传时，后端会自动生成一个 hash
- `device_name` 不传时默认 `Unknown Device`
- `platform` 不传时默认 `android`
- `push_token` 为空时不会写入推送 token 表

### 1.6 图片状态定义

后端输出给 App 的图片状态：

- `approved = 1` -> `approved`
- `approved = 2` -> `rejected`
- 其他值 -> `pending`

补充说明：

- “我的图片”列表里，待审核实际是数据库 `approved IN (0, 3)`
- 但返回给 App 时，这两种都会统一表现为 `pending`

## 2. 认证接口

### 2.1 登录

- 方法：`POST`
- 路径：`/api/app/v1/auth/login.php`
- 是否需要登录：否

请求字段：

- `login`：用户名或邮箱，必填
- `password`：密码，必填
- `device_id`：可选
- `device_name`：可选
- `platform`：可选，默认 `android`
- `system_version`：可选
- `app_version`：可选
- `push_token`：可选

成功返回：

```json
{
  "success": true,
  "user": {
    "id": 123,
    "username": "demo",
    "email": "demo@example.com",
    "email_verified": true,
    "is_admin": false,
    "sys_admin": false
  },
  "auth": {
    "session_id": 88,
    "access_token": "xxx",
    "refresh_token": "yyy",
    "access_token_expires_at": "2026-03-12 15:00:00",
    "refresh_token_expires_at": "2026-04-11 13:00:00",
    "device": {
      "id": 88,
      "device_id": "android-001",
      "device_name": "Pixel 8",
      "platform": "android",
      "system_version": "14",
      "app_version": "1.0.0",
      "is_current": true
    }
  }
}
```

失败要点：

- 用户名/邮箱或密码错误：`401`
- 邮箱未验证：`403`
- 账号被封禁：`403`

### 2.2 注册

- 方法：`POST`
- 路径：`/api/app/v1/auth/register.php`
- 是否需要登录：否

请求字段：

- `username`：必填
- `email`：必填
- `password`：必填，最少 `6` 位
- `password_confirm`：必填

成功返回：

```json
{
  "success": true,
  "message": "注册成功，请先验证邮箱"
}
```

状态码：

- 成功时为 `201`

失败要点：

- 两次密码不一致：`422`
- 密码少于 6 位：`422`
- 用户名或邮箱已存在：`422`
- 用户记录已写入，但验证邮件发送失败：`500`

注意：

- 注册后不会自动登录
- 必须先完成邮箱验证，才能登录 App

### 2.3 刷新 Token

- 方法：`POST`
- 路径：`/api/app/v1/auth/refresh.php`
- 是否需要登录：否

请求字段：

- `refresh_token`：必填
- `push_token`：可选，如果传了会绑定到新 session

成功返回：

- 返回结构和登录接口中的 `user + auth` 一样

失败要点：

- refresh token 无效或过期：`401`
- 用户被封禁或不存在：`403`

重要行为：

- 刷新成功后，旧 session 立刻 `revoked`
- 安卓端必须保存新返回的 `access_token` 和 `refresh_token`
- 旧 refresh token 不应再继续使用

### 2.4 退出当前设备

- 方法：`POST`
- 路径：`/api/app/v1/auth/logout.php`
- 是否需要登录：是

成功返回：

```json
{
  "success": true,
  "message": "已退出当前设备"
}
```

### 2.5 退出其他设备

- 方法：`POST`
- 路径：`/api/app/v1/auth/logout_others.php`
- 是否需要登录：是

成功返回：

```json
{
  "success": true,
  "message": "已退出其他设备"
}
```

### 2.6 设备列表

- 方法：`GET`
- 路径：`/api/app/v1/auth/devices.php`
- 是否需要登录：是

成功返回：

```json
{
  "success": true,
  "items": [
    {
      "id": 88,
      "device_id": "android-001",
      "device_name": "Pixel 8",
      "platform": "android",
      "system_version": "14",
      "app_version": "1.0.0",
      "ip_address": "1.2.3.4",
      "created_at": "2026-03-12 10:00:00",
      "last_seen_at": "2026-03-12 10:30:00",
      "revoked_at": null,
      "is_current": true,
      "is_active": true
    }
  ]
}
```

字段说明：

- `id`：session ID，后续踢设备时就用这个
- `is_current`：是不是当前设备
- `is_active`：当前实现等价于 `revoked_at == null`

### 2.7 下线指定设备

- 方法：`POST`
- 路径：`/api/app/v1/auth/revoke_device.php`
- 是否需要登录：是

请求字段：

- `session_id`：目标 session ID，必填

成功返回：

```json
{
  "success": true,
  "message": "设备已下线"
}
```

失败要点：

- 不能踢自己当前设备：`422`

说明：

- 如果传入的 `session_id` 不属于当前用户，后端不会报 404，只是不会更新任何记录
- 安卓端如果需要更强反馈，调用后建议刷新设备列表确认结果

### 2.8 修改密码

- 方法：`POST`
- 路径：`/api/app/v1/auth/change_password.php`
- 是否需要登录：是

请求字段：

- `old_password`：必填
- `new_password`：必填，最少 `6` 位
- `new_password_confirm`：必填

成功返回：

```json
{
  "success": true,
  "message": "密码修改成功"
}
```

重要行为：

- 修改密码后，后端会把“当前 session 以外”的所有 session 全部下线
- 当前设备保持登录状态

### 2.9 忘记密码 - 发送验证码

- 方法：`POST`
- 路径：`/api/app/v1/auth/forgot_password_request.php`
- 是否需要登录：否

请求字段：

- `email`：必填

成功返回：

```json
{
  "success": true,
  "message": "验证码已发送",
  "expires_in": 300
}
```

规则：

- 邮件验证码是 `6 位数字`
- 有效期 `5 分钟`
- 同一邮箱或同一 IP，`1 小时` 内最多请求 `10` 次

失败要点：

- 邮箱不存在：`404`
- 请求过于频繁：`429`
- 发信失败：`500`

### 2.10 忘记密码 - 重置密码

- 方法：`POST`
- 路径：`/api/app/v1/auth/forgot_password_reset.php`
- 是否需要登录：否

请求字段：

- `email`：必填
- `code`：邮箱收到的 6 位验证码，必填
- `new_password`：必填
- `new_password_confirm`：必填

成功返回：

```json
{
  "success": true,
  "message": "密码已重置"
}
```

规则：

- 验证码只取该邮箱最近一条、且未消费的记录
- 码过期直接失败
- 连续输错 `5` 次后锁定 `1 小时`
- 重置成功后，会把该用户所有 App session 全部下线

## 3. 图片接口

### 3.1 公共图片流

- 方法：`GET`
- 路径：`/api/app/v1/photos/feed.php`
- 是否需要登录：否

支持查询参数：

- `page`：页码，默认 `1`
- `per_page`：每页数量，默认 `30`，最大 `60`
- `iatacode`：拍摄地点 IATA，大写更稳妥
- `userid`：作者用户 ID
- `airline`：航司，实际映射数据库 `category`
- `aircraft_model`：机型
- `cam`：相机
- `lens`：镜头
- `registration_number`：注册号，后端会按大写处理
- `keyword`：关键字，模糊匹配标题、用户名、航司、机型、注册号、相机、镜头、拍摄地点

排序规则：

- `score DESC`
- 然后 `created_at DESC`

成功返回：

```json
{
  "success": true,
  "page": 1,
  "per_page": 30,
  "total": 1000,
  "has_more": true,
  "items": [
    {
      "id": 1,
      "title": "B-1234 at PEK",
      "username": "demo",
      "user_id": 123,
      "location": "PEK",
      "airline": "Air China",
      "aircraft_model": "A320",
      "registration_number": "B-1234",
      "cam": "Canon EOS R6",
      "lens": "RF100-500",
      "detail_url": "photo_detail.php?id=1",
      "author_url": "author.php?userid=123",
      "thumb_url": "photo_asset.php?id=1&variant=thumb&expires=...&sig=...",
      "original_url": "photo_asset.php?id=1&variant=original&expires=...&sig=...",
      "status": "approved",
      "approved_code": 1,
      "score": 150,
      "views": 1000,
      "likes": 66,
      "liked": false,
      "author_name": "demo",
      "reviewer_name": "",
      "created_at": "2026-03-01 10:00:00",
      "shooting_time": "2026:02:28 17:00:00",
      "shooting_location": "PEK",
      "camera": "Canon EOS R6",
      "lens_model": "RF100-500",
      "focal_length": 500,
      "iso": 400,
      "aperture": 7.1,
      "shutter": "1/1000",
      "rejection_reason": null,
      "admin_comment": null,
      "share_url": "https://www.syphotos.cn/photo_detail.php?id=1",
      "allow_use": 1,
      "is_featured": 0,
      "dimensions": {
        "original_width": 6000,
        "original_height": 4000,
        "final_width": 1600,
        "final_height": 1067
      }
    }
  ]
}
```

注意：

- 这里只返回 `approved = 1` 的图片
- 如果请求头里带了有效 Bearer token，会额外计算每张图的 `liked`
- 不带 token 也能访问，只是 `liked` 一般为 `false`

### 3.2 图片详情

- 方法：`GET`
- 路径：`/api/app/v1/photos/detail.php`
- 是否需要登录：否

请求参数：

- `id`：图片 ID，必填

成功返回：

```json
{
  "success": true,
  "item": {
    "...": "字段与 feed 单个 item 基本一致"
  }
}
```

权限规则：

- 普通游客只能看 `approved = 1`
- 已登录用户还能看“自己上传但尚未过审”的图片详情

失败要点：

- 缺少图片 ID：`422`
- 图片不存在或无权查看：`404`

### 3.3 点赞 / 取消点赞

- 方法：`POST`
- 路径：`/api/app/v1/photos/toggle_like.php`
- 是否需要登录：是

请求字段：

- `photo_id`：必填

成功返回：

```json
{
  "success": true,
  "liked": true,
  "likes": 67
}
```

规则：

- 已点赞则取消
- 未点赞则新增
- 只能操作“已通过图片”或“自己的图片”

失败要点：

- 图片不存在或无权操作：`404`

### 3.4 我的图片

- 方法：`GET`
- 路径：`/api/app/v1/photos/my.php`
- 是否需要登录：是

查询参数：

- `status`：`all | approved | pending | rejected`，默认 `all`
- `page`：默认 `1`
- `per_page`：默认 `30`，最大 `60`

成功返回：

- 分页结构与 `feed.php` 一样

状态规则：

- `approved` -> `approved = 1`
- `rejected` -> `approved = 2`
- `pending` -> `approved IN (0, 3)`
- `all` -> 不加状态过滤

### 3.5 我点赞过的图片

- 方法：`GET`
- 路径：`/api/app/v1/photos/likes.php`
- 是否需要登录：是

查询参数：

- `page`：默认 `1`
- `per_page`：默认 `30`，最大 `60`

成功返回：

- 分页结构与 `feed.php` 一样

规则：

- 这里只会返回当前仍然是 `approved = 1` 的图片

### 3.6 修改待审核图片信息

- 方法：`POST`
- 路径：`/api/app/v1/photos/update_pending.php`
- 是否需要登录：是

请求字段：

- `photo_id`：必填

可更新字段：

- `title`
- `category`
- `aircraft_model`
- `registration_number`
- `shooting_time`
- `shooting_location`
- `camera`
- `lens_model`
- `shutter`
- `allow_use`
- `FocalLength`
- `ISO`
- `aperture`

字段映射说明：

- `category` -> 数据库 `category`
- `shooting_time` -> 数据库 ``拍摄时间``
- `shooting_location` -> 数据库 ``拍摄地点``
- `camera` -> 数据库 `Cam`
- `lens_model` -> 数据库 `Lens`
- `aperture` -> 数据库 `F`

特殊处理：

- `registration_number` 会自动转大写
- `shooting_location` 会自动转大写
- `allow_use`、`FocalLength`、`ISO` 为空字符串时会写 `null`
- `aperture` 为空字符串时会写 `null`

成功返回：

```json
{
  "success": true,
  "message": "图片信息已更新",
  "item": {
    "...": "更新后的图片详情"
  }
}
```

限制：

- 只有图片 owner 才能改
- 只有 `approved = 0` 的图片能改
- `approved = 3` 虽然在“我的待审核”列表里也会被归到 pending，但这个接口不允许修改

失败要点：

- 图片不存在：`404`
- 不是待审核图片：`422`
- 没有任何可更新字段：`422`

### 3.7 删除图片

- 方法：`POST`
- 路径：`/api/app/v1/photos/delete.php`
- 是否需要登录：是

请求字段：

- `photo_id`：必填
- `title_confirm`：必填，必须与数据库中的 `title` 完全一致（代码中是 `trim` 后比较）

成功返回：

```json
{
  "success": true,
  "message": "图片已删除"
}
```

删除行为：

- 删除 `photo_likes`
- 删除 `appeals`
- 删除 `photos`
- 事务提交后再删除磁盘文件：
  - `uploads/{filename}`
  - `uploads/o/{filename}`

失败要点：

- 图片不存在：`404`
- 确认标题不匹配：`422`

## 4. 搜索、分类、地图、个人信息

### 4.1 搜索建议

- 方法：`GET`
- 路径：`/api/app/v1/search/suggestions.php`
- 是否需要登录：否

查询参数：

- `field`：必填，可选值：
  - `userid`
  - `airline`
  - `aircraft_model`
  - `cam`
  - `lens`
  - `registration_number`
  - `iatacode`
- `q`：可选，模糊输入内容
- 还支持 `feed.php` 那一套筛选参数，后端会在当前筛选上下文内给建议

成功返回：

```json
{
  "success": true,
  "items": [
    {
      "value": "Air China",
      "label": "Air China",
      "count": 123
    }
  ]
}
```

规则：

- 最多返回 `10` 条
- `userid` 返回的是用户 ID 作为 `value`，用户名作为 `label`
- 其他字段一般 `value` 和 `label` 一样

### 4.2 分类统计

- 方法：`GET`
- 路径：`/api/app/v1/categories/counts.php`
- 是否需要登录：否

查询参数：

- `type`：`airline` 或 `aircraft_model`，默认 `airline`
- `page`：默认 `1`
- `per_page`：默认 `100`，最大 `100`

成功返回：

```json
{
  "success": true,
  "page": 1,
  "per_page": 100,
  "total": 200,
  "has_more": true,
  "items": [
    {
      "label": "Air China",
      "count": 999
    }
  ]
}
```

数据来源说明：

- 不是 `photos` 表
- 实际查询的是 `airplane` 表
- `type=airline` 时取 `operator`
- `type=aircraft_model` 时取 `modes`

### 4.3 地图聚合

- 方法：`GET`
- 路径：`/api/app/v1/map/clusters.php`
- 是否需要登录：否

查询参数：

- `level`：`country | province | city`，默认 `country`
- 还支持 `feed.php` 相同的筛选参数

成功返回：

```json
{
  "success": true,
  "items": [
    {
      "level": "country",
      "key": "CN",
      "name": "CN",
      "latitude": 39.9,
      "longitude": 116.4,
      "photo_count": 1234
    }
  ]
}
```

规则：

- 只统计 `approved = 1` 图片
- 通过 `photos.拍摄地点 = airport.iata_code` 关联机场表
- 聚合坐标使用该分组下机场经纬度的平均值

### 4.4 个人首页摘要

- 方法：`GET`
- 路径：`/api/app/v1/me/summary.php`
- 是否需要登录：是

成功返回：

```json
{
  "success": true,
  "user": {
    "id": 123,
    "username": "demo",
    "email": "demo@example.com",
    "email_verified": true,
    "is_admin": false,
    "sys_admin": false
  },
  "stats": {
    "all_photos": 10,
    "approved_photos": 8,
    "pending_photos": 1,
    "rejected_photos": 1,
    "liked_photos": 20,
    "unread_notifications": 3
  }
}
```

## 5. 通知接口

路径统一：

- `/api/app/v1/notifications/index.php`

### 5.1 获取通知列表

- 方法：`GET`
- 是否需要登录：是

查询参数：

- `page`：默认 `1`
- `per_page`：默认 `20`，最大 `50`

成功返回：

```json
{
  "success": true,
  "page": 1,
  "per_page": 20,
  "total": 5,
  "has_more": false,
  "items": [
    {
      "id": 1,
      "type": "review_result",
      "title": "审核结果通知",
      "body": "你的图片已通过审核",
      "payload": {
        "photo_id": 123
      },
      "is_read": false,
      "created_at": "2026-03-12 09:00:00",
      "read_at": null
    }
  ]
}
```

说明：

- `payload_json` 会被 `json_decode` 成 `payload`
- 如果库里是空值，返回 `payload: null`

### 5.2 标记已读

- 方法：`POST`
- 是否需要登录：是

请求字段：

- `notification_id`：可选

规则：

- 传 `notification_id`：只标记该条
- 不传或传 `0`：标记当前用户全部未读通知

成功返回：

```json
{
  "success": true,
  "message": "通知已标记已读"
}
```

## 6. 图片资源与辅助接口

### 6.1 图片资源地址

图片列表和详情里返回的 `thumb_url` / `original_url`，本质上都是：

- `/photo_asset.php?id={photo_id}&variant={thumb|original}&expires={unix_ts}&sig={signature}`

说明：

- 这是一个带签名、带过期时间的临时资源地址
- `variant` 只支持 `thumb` 和 `original`
- 过期后会返回 `403`
- 签名不正确也会返回 `403`

安卓端建议：

- 不要自己拼签名
- 直接使用 feed/detail 返回的现成 URL
- URL 过期后，重新请求 feed/detail 获取新地址

### 6.2 IATA 查询机场名称

- 方法：`GET`
- 路径：`/api/GetAirPortName.php`
- 是否需要登录：否

查询参数：

- `code`：IATA 机场代码，必填

成功返回：

```json
{
  "iata_code": "PEK",
  "airport_name": "Beijing Capital International Airport"
}
```

失败：

- 缺参数时 `400`
- 返回格式不是 `success/error` 包装，而是单独字段

### 6.3 通过注册号查询飞机信息

- 方法：`GET`
- 路径：`/api/plane-info.php`
- 是否需要登录：否

查询参数：

- `registration`：注册号，必填，最少 3 个字符

成功返回：

```json
{
  "status": "success",
  "data": {
    "机型": "A320",
    "运营机构": "Air China"
  }
}
```

未找到：

```json
{
  "status": "not_found",
  "message": "未找到该飞机信息"
}
```

失败：

```json
{
  "status": "error",
  "message": "..."
}
```

注意：

- 这个接口不是 App API 风格
- 返回字段是 `status`，不是 `success`
- `data` 里的 key 是中文

## 7. 旧接口与不建议安卓直接使用的接口

### 7.1 旧版图片流

- `/api/photo_feed.php`
- `/api/photo_filter_suggest.php`

说明：

- 这是网站前端使用的旧接口
- `photo_feed.php` 需要签名参数 `expires + sig`
- 还会返回一份 `html` 字符串，明显偏 Web 场景
- 安卓端应优先使用 `/api/app/v1/photos/feed.php` 和 `/api/app/v1/search/suggestions.php`

## 8. 当前未实现的安卓能力

这部分很关键，安卓端不要假设已经有：

- 没有 `api/app/v1/photos/upload.php`
- 没有 `api/app/v1/photos/create.php`
- 没有 App 专用的 `multipart/form-data` 上传接口
- 没有评论接口
- 没有收藏夹接口
- 没有通知删除接口
- 没有通知未读数单独接口，当前应从 `/api/app/v1/me/summary.php` 或通知列表推导

上传相关现状：

- 网站已有 `upload.php` 页面和表单提交流程
- 但它是传统网页表单，不是安卓 App 可直接复用的 REST API
- 如果安卓需要原生上传，后端还必须新增专门接口

## 9. 安卓对接建议

### 9.1 认证流

推荐流程：

1. 登录拿 `access_token + refresh_token`
2. 所有受保护接口带 `Authorization: Bearer {access_token}`
3. 收到 `401` 后尝试调用刷新接口
4. 刷新成功后重放原请求
5. 刷新失败则回登录页

### 9.2 图片展示

推荐：

- 首页和搜索都走 `/api/app/v1/photos/feed.php`
- 图片详情走 `/api/app/v1/photos/detail.php`
- 缩略图 / 原图直接加载接口返回的资源 URL

### 9.3 我的页面

推荐：

- 顶部统计走 `/api/app/v1/me/summary.php`
- 我的作品走 `/api/app/v1/photos/my.php`
- 我的点赞走 `/api/app/v1/photos/likes.php`
- 设备管理走 `auth/devices.php + auth/revoke_device.php`

### 9.4 待审核编辑

当前真实规则：

- 只能修改 `approved = 0`
- 不是所有 pending 都能编辑
- 所以安卓端应以服务端返回结果为准，不要只靠 `status == pending` 就开放编辑入口

## 10. 最后结论

如果按当前仓库代码来接，安卓端已经可以稳定接入的模块有：

- 注册、登录、刷新、退出、设备管理、忘记密码
- 图片流、图片详情、点赞、我的图片、我的点赞、删除图片、修改待审图片信息
- 搜索建议、分类统计、地图聚合、通知、个人摘要

当前还不能直接接入的核心模块：

- 原生上传

如果后续要补安卓上传接口，建议新增独立的 App 端上传 API，而不是让安卓去模拟网页 `upload.php`。
