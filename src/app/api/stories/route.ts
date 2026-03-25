import { NextResponse } from "next/server";
import db, { initDatabase } from "@/lib/db";

initDatabase();

export async function GET() {
  const stories = db.prepare("SELECT * FROM stories ORDER BY created_at DESC").all();
  return NextResponse.json(stories);
}

export async function POST(request: Request) {
  try {
    const data = await request.json();
    const { title, content, cover_url, location, photo_ids, route_ids } = data;
    if (!title || !content) return NextResponse.json({ error: "标题和内容不能为空" }, { status: 400 });
    const result = db.prepare("INSERT INTO stories (title, content, cover_url, location, photo_ids, route_ids) VALUES (?, ?, ?, ?, ?, ?)")
      .run(title, content, cover_url || null, location || null, photo_ids ? JSON.stringify(photo_ids) : null, route_ids ? JSON.stringify(route_ids) : null);
    return NextResponse.json({ id: result.lastInsertRowid, ...data });
  } catch (error) {
    return NextResponse.json({ error: "创建失败" }, { status: 500 });
  }
}
