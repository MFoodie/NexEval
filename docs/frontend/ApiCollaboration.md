# NexEval 前后端协作规范

## 1. Axios 封装
- 单实例 axios，统一配置 baseURL 和 timeout
- baseURL 来源 VITE_API_BASE
- 默认超时 10 秒

## 2. JWT 鉴权
- token 存储在 sessionStorage
- 请求拦截：自动注入 Authorization: Bearer {token}
- 响应拦截：401 自动清理 token 并跳转 /login

## 3. 响应格式
统一返回结构：
{
  "code": 0,
  "message": "OK",
  "data": {}
}

- code = 0 成功
- code != 0 失败

## 4. 错误码约定
- 400：参数校验错误
- 401：鉴权失效
- 403：无权限
- 404：资源不存在
- 500：系统错误

## 5. 错误处理
- 网络异常：全局 toast
- 业务错误：表单内提示 + toast
- 异常日志：统一上报

## 6. WebSocket 约定
- 请求格式：
  { type: "REQUEST", requestId, action, payload }
- 响应格式：
  { type: "RESPONSE", requestId, success, payload | error }
- 事件格式：
  { type: "EVENT", event, payload }
