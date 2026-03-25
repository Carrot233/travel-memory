# 旅途记忆 - 个人旅游网站

一个用于分享旅游照片、路线、视频和旅行故事的个人网站。

![Next.js](https://img.shields.io/badge/Next.js-16.2-black)
![React](https://img.shields.io/badge/React-18-blue)
![TypeScript](https://img.shields.io/badge/TypeScript-5-blue)

## 功能特性

- 📷 **照片管理** - 上传、浏览、删除旅游照片
- 🗺️ **路线规划** - 创建旅游路线，添加景点
- 🎬 **视频分享** - 上传和展示旅行视频
- 📝 **故事记录** - 撰写旅行日记和故事
- 👍 **互动功能** - 点赞、评论
- 🎨 **后台管理** - 完整的管理后台
- 🌙 **深色模式** - 支持深色/浅色主题

## 技术栈

- **前端**: Next.js 16 (Turbopack), React 18, TypeScript
- **样式**: Tailwind CSS
- **后端**: Next.js API Routes
- **数据库**: SQLite (better-sqlite3)
- **地图**: 高德地图 Web API
- **部署**: 支持 Vercel / 腾讯云 / 服务器

## 环境要求

- Node.js 22.x 或更高版本
- npm 10.x 或更高版本

## 本地开发

### 1. 克隆项目

```bash
git clone https://github.com/Carrot233/travel-memory.git
cd travel-memory
```

### 2. 安装依赖

```bash
npm install
```

### 3. 配置环境变量（可选）

如需修改配置，可创建 `.env.local` 文件：

```bash
# 高德地图 API Key（如需修改）
AMAP_KEY=your_amap_key_here
```

> 注意：高德地图 API Key 已预配置，如需更换请到 [高德开放平台](https://lbs.amap.com/) 申请。

### 4. 启动开发服务器

```bash
npm run dev
```

打开浏览器访问：http://localhost:3000

### 5. 登录后台

- 访问：http://localhost:3000/admin
- 默认账号：`admin`
- 默认密码：`admin123`

## 生产部署

### 方式一：Vercel（推荐）

1. 将代码推送到 GitHub
2. 访问 [Vercel](https://vercel.com)
3. Import Git Repository
4. Deploy 即可

### 方式二：服务器部署

```bash
# 1. 安装 Node.js 22.x
curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
sudo apt-get install -y nodejs

# 2. 克隆并安装
git clone https://github.com/Carrot233/travel-memory.git
cd travel-memory
npm install

# 3. 构建生产版本
npm run build

# 4. 使用 PM2 启动
npm install -g pm2
pm2 start npm --name "travel-site" -- run start

# 5. 配置 Nginx 反向代理（可选）
# 参考下方 Nginx 配置
```

### Nginx 配置示例

```nginx
server {
    listen 80;
    server_name your-domain.com; # 或服务器 IP

    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }
}
```

### 腾讯云/阿里云安全组

如使用云服务器，需开放端口：
- 80 (HTTP)
- 443 (HTTPS，如使用SSL)
- 3000 (Node.js 应用)

## 项目结构

```
travel-site/
├── src/
│   ├── app/                 # Next.js App Router
│   │   ├── api/            # API 路由
│   │   ├── admin/         # 管理后台页面
│   │   ├── photos/       # 照片页面
│   │   ├── routes/       # 路线页面
│   │   ├── videos/       # 视频页面
│   │   └── stories/      # 故事页面
│   ├── components/        # React 组件
│   ├── lib/              # 工具函数和配置
│   └── types/            # TypeScript 类型定义
├── public/               # 静态资源
│   └── uploads/          # 上传的图片/视频
├── data/                 # SQLite 数据库
└── package.json
```

## 常用命令

```bash
npm run dev      # 启动开发服务器
npm run build    # 构建生产版本
npm run start    # 启动生产服务器
npm run lint     # 代码检查
```

## 许可证

MIT License
