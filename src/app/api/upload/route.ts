import { NextRequest, NextResponse } from "next/server";
import { writeFile, mkdir } from "fs/promises";
import { existsSync } from "fs";
import path from "path";

export async function POST(request: NextRequest) {
  try {
    const formData = await request.formData();
    const file = formData.get("file") as File | null;
    const type = formData.get("type") as string || "photos"; // photos, videos, thumbnails

    if (!file) {
      return NextResponse.json({ error: "未找到文件" }, { status: 400 });
    }

    // 验证文件类型
    const allowedImageTypes = ["image/jpeg", "image/png", "image/gif", "image/webp"];
    const allowedVideoTypes = ["video/mp4", "video/webm", "video/ogg"];
    
    const isImage = allowedImageTypes.includes(file.type);
    const isVideo = allowedVideoTypes.includes(file.type);
    
    if (type === "photos" && !isImage) {
      return NextResponse.json({ error: "只支持 JPG、PNG、GIF、WEBP 格式的图片" }, { status: 400 });
    }
    
    if (type === "videos" && !isVideo) {
      return NextResponse.json({ error: "只支持 MP4、WEBM、OGG 格式的视频" }, { status: 400 });
    }
    
    if (type === "thumbnails" && !isImage) {
      return NextResponse.json({ error: "封面图必须是图片格式" }, { status: 400 });
    }

    // 生成文件名：时间戳_随机数.扩展名
    const ext = file.name.split(".").pop() || "bin";
    const timestamp = Date.now();
    const random = Math.random().toString(36).substring(2, 8);
    const fileName = `${timestamp}_${random}.${ext}`;

    // 确保目录存在
    const uploadDir = path.join(process.cwd(), "public", "uploads", type);
    if (!existsSync(uploadDir)) {
      await mkdir(uploadDir, { recursive: true });
    }

    // 写入文件
    const filePath = path.join(uploadDir, fileName);
    const bytes = await file.arrayBuffer();
    const buffer = Buffer.from(bytes);
    await writeFile(filePath, buffer);

    // 返回可访问的 URL（使用 API 路由以确保生产模式下可访问）
    const url = `/api/uploads/${type}/${fileName}`;

    return NextResponse.json({
      success: true,
      url,
      fileName,
      size: file.size,
      type: file.type,
    });
  } catch (error) {
    console.error("上传错误:", error);
    return NextResponse.json({ error: "上传失败" }, { status: 500 });
  }
}
