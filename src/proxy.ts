import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { isBlockedUploadPath } from "./lib/media-upload";

const BLOCK_HEADERS = {
  "X-Content-Type-Options": "nosniff",
  "Content-Security-Policy": "default-src 'none'; sandbox",
};

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const inUploadSpace =
    pathname.startsWith("/uploads/") || pathname.startsWith("/api/uploads/");

  if (!inUploadSpace) {
    return NextResponse.next();
  }

  if (isBlockedUploadPath(pathname)) {
    return NextResponse.json({ error: "文件不存在" }, { status: 404, headers: BLOCK_HEADERS });
  }

  if (pathname.startsWith("/uploads/")) {
    const url = request.nextUrl.clone();
    url.pathname = `/api${pathname}`;
    return NextResponse.rewrite(url);
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/uploads/:path*", "/api/uploads/:path*"],
};
