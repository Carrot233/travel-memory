import { NextRequest, NextResponse } from "next/server";
import { saveUpload } from "@/lib/media-storage";
import {
  buildStoredFileName,
  isUploadCategory,
  validateMediaUpload,
} from "@/lib/media-upload";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function POST(request: NextRequest) {
  try {
    const formData = await request.formData();
    const file = formData.get("file");
    const typeValue = formData.get("type");
    const type = typeof typeValue === "string" && typeValue ? typeValue : "photos";

    if (!(file instanceof File)) {
      return NextResponse.json({ error: "未找到文件" }, { status: 400 });
    }

    if (!isUploadCategory(type)) {
      return NextResponse.json({ error: "不支持的上传类型" }, { status: 400 });
    }

    const verdict = await validateMediaUpload(file, type);
    if (!verdict.ok) {
      return NextResponse.json({ error: verdict.error }, { status: 400 });
    }

    const fileName = buildStoredFileName(verdict.extension);
    await saveUpload(type, fileName, verdict.buffer);

    const url = `/api/uploads/${type}/${fileName}`;
    return NextResponse.json({
      success: true,
      url,
      fileName,
      size: verdict.buffer.length,
      type: verdict.contentType,
    });
  } catch (error) {
    console.error("上传错误:", error);
    return NextResponse.json({ error: "上传失败" }, { status: 500 });
  }
}
