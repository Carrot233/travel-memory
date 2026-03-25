import { NextRequest, NextResponse } from "next/server";
import db, { initDatabase } from "@/lib/db";

initDatabase();

// 获取点赞数
export async function GET(request: NextRequest) {
  const { searchParams } = new URL(request.url);
  const targetType = searchParams.get("target_type");
  const targetId = searchParams.get("target_id");

  if (!targetType || !targetId) {
    return NextResponse.json({ count: 0 });
  }

  const result = db
    .prepare("SELECT COUNT(*) as count FROM likes WHERE target_type = ? AND target_id = ?")
    .get(targetType, targetId) as { count: number };

  return NextResponse.json({ count: result.count });
}

// 点赞
export async function POST(request: NextRequest) {
  const cookie = request.cookies.get("session");
  if (!cookie) {
    return NextResponse.json({ error: "请先登录" }, { status: 401 });
  }

  try {
    const session = JSON.parse(Buffer.from(cookie.value, "base64").toString());
    const { target_type, target_id } = await request.json();

    db.prepare("INSERT INTO likes (user_id, target_type, target_id) VALUES (?, ?, ?)").run(
      session.id,
      target_type,
      target_id
    );

    return NextResponse.json({ success: true });
  } catch (error) {
    return NextResponse.json({ error: "已点赞或操作失败" }, { status: 400 });
  }
}

// 取消点赞
export async function DELETE(request: NextRequest) {
  const cookie = request.cookies.get("session");
  if (!cookie) {
    return NextResponse.json({ error: "请先登录" }, { status: 401 });
  }

  try {
    const session = JSON.parse(Buffer.from(cookie.value, "base64").toString());
    const { searchParams } = new URL(request.url);
    const targetType = searchParams.get("target_type");
    const targetId = searchParams.get("target_id");

    db.prepare("DELETE FROM likes WHERE user_id = ? AND target_type = ? AND target_id = ?").run(
      session.id,
      targetType,
      targetId
    );

    return NextResponse.json({ success: true });
  } catch (error) {
    return NextResponse.json({ error: "操作失败" }, { status: 400 });
  }
}
