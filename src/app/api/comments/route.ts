import { NextRequest, NextResponse } from "next/server";
import db, { initDatabase } from "@/lib/db";

initDatabase();

// 获取评论列表
export async function GET(request: NextRequest) {
  const { searchParams } = new URL(request.url);
  const targetType = searchParams.get("target_type");
  const targetId = searchParams.get("target_id");

  if (!targetType || !targetId) {
    return NextResponse.json([]);
  }

  const comments = db
    .prepare(`
      SELECT c.*, u.username 
      FROM comments c 
      JOIN users u ON c.user_id = u.id 
      WHERE c.target_type = ? AND c.target_id = ?
      ORDER BY c.created_at DESC
    `)
    .all(targetType, targetId);

  return NextResponse.json(comments);
}

// 发表评论
export async function POST(request: NextRequest) {
  const cookie = request.cookies.get("session");
  if (!cookie) {
    return NextResponse.json({ error: "请先登录" }, { status: 401 });
  }

  try {
    const session = JSON.parse(Buffer.from(cookie.value, "base64").toString());
    const { target_type, target_id, content } = await request.json();

    if (!content || !content.trim()) {
      return NextResponse.json({ error: "评论内容不能为空" }, { status: 400 });
    }

    const result = db
      .prepare("INSERT INTO comments (user_id, target_type, target_id, content) VALUES (?, ?, ?, ?)")
      .run(session.id, target_type, target_id, content);

    return NextResponse.json({
      id: result.lastInsertRowid,
      user_id: session.id,
      username: session.username,
      target_type,
      target_id,
      content,
      created_at: new Date().toISOString(),
    });
  } catch (error) {
    console.error("评论失败:", error);
    return NextResponse.json({ error: "评论失败" }, { status: 500 });
  }
}
