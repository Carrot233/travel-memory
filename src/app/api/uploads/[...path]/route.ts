import { NextRequest, NextResponse } from "next/server";
import { readStoredMedia } from "@/lib/media-storage";
import { mediaResponseHeaders, sniffMedia } from "@/lib/media-upload";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function GET(
  _request: NextRequest,
  { params }: { params: Promise<{ path: string[] }> }
) {
  try {
    const { path: pathSegments } = await params;
    const located = await readStoredMedia(pathSegments);
    if (!located || sniffMedia(located.buffer) !== located.contentType) {
      return missing();
    }

    return new NextResponse(located.buffer as unknown as BodyInit, {
      headers: mediaResponseHeaders(located.contentType, located.fileName),
    });
  } catch (error) {
    console.error("读取文件失败:", error);
    return NextResponse.json(
      { error: "读取文件失败" },
      { status: 500, headers: { "X-Content-Type-Options": "nosniff" } }
    );
  }
}

function missing() {
  return NextResponse.json(
    { error: "文件不存在" },
    {
      status: 404,
      headers: {
        "X-Content-Type-Options": "nosniff",
        "Content-Security-Policy": "default-src 'none'; sandbox",
      },
    }
  );
}
