import { NextRequest, NextResponse } from "next/server";
import db, { initDatabase } from "@/lib/db";

initDatabase();

export async function GET(request: NextRequest) {
  const cookie = request.cookies.get("session");
  if (!cookie) {
    return NextResponse.json({ liked: false });
  }

  try {
    const session = JSON.parse(Buffer.from(cookie.value, "base64").toString());
    const { searchParams } = new URL(request.url);
    const targetType = searchParams.get("target_type");
    const targetId = searchParams.get("target_id");

    const result = db
      .prepare("SELECT id FROM likes WHERE user_id = ? AND target_type = ? AND target_id = ?")
      .get(session.id, targetType, targetId);

    return NextResponse.json({ liked: !!result });
  } catch {
    return NextResponse.json({ liked: false });
  }
}
