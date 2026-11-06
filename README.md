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

### 3. 配置环境变量

必须创建 `.env.local` 文件配置高德地图 API Key：

```bash
# 创建环境变量文件
cp .env.example .env.local

# 编辑 .env.local，填入你的高德地图 Key
AMAP_KEY=你的高德地图API密钥
```

获取高德地图 API Key：
1. 访问 [高德开放平台](https://lbs.amap.com/)
2. 注册/登录后创建应用
3. 添加 Web端JS API 服务
4. 复制 Key 填入上方配置

### 4. 启动开发服务器

```bash
npm run dev
```

打开浏览器访问：http://localhost:3000

### 5. 登录后台

- 访问：http://localhost:3000/admin
- 默认账号：`admin`
- 默认密码：`admin123`
- 部署到公网前请修改默认管理员密码

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

    client_max_body_size 200m;

    # 全部请求（含上传文件）都交给 Next.js。不要为该站点启用 PHP 或其他脚本解释器。
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
│   └── uploads/          # 历史上传的图片（仅作媒体读取）
├── data/                 # SQLite 数据库与新上传的媒体
│   └── uploads/          # 新上传的图片/视频（不在静态目录中）
└── package.json
```

## 上传文件

照片和视频只接受真实媒体：图片为 JPG、PNG、GIF、WEBP（不超过 20MB），视频为 MP4、WEBM、OGG（不超过 200MB）。服务端会同时检查扩展名和文件内容，不信任浏览器自行声明的类型。脚本类扩展名会被拒绝。

新文件保存在 `data/uploads/`，通过应用以对应的媒体类型返回（`Content-Disposition: inline`，并禁止内容嗅探）。该目录不在 `public/` 下，不会被当作网站脚本执行。请不要把 `data/uploads/` 里的个人媒体提交到仓库。

如果本机 `public/uploads/` 中有并非照片或视频的脚本文件（例如扩展名是 `.php`），请在确认后删除，不要提交。

## 常用命令

```bash
npm run dev      # 启动开发服务器
npm run build    # 构建生产版本
npm run start    # 启动生产服务器
npm run lint     # 代码检查
npm run test     # 上传校验测试
```

## 许可证

MIT License
