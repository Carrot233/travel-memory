import { NextResponse } from "next/server";
import db, { initDatabase } from "@/lib/db";

initDatabase();

export async function GET() {
  const videos = db.prepare("SELECT * FROM videos ORDER BY created_at DESC").all();
  return NextResponse.json(videos);
}

export async function POST(request: Request) {
  try {
    const data = await request.json();
    const { title, description, url, thumbnail_url, duration } = data;
    if (!title || !url) return NextResponse.json({ error: "标题和URL不能为空" }, { status: 400 });
    const result = db.prepare("INSERT INTO videos (title, description, url, thumbnail_url, duration) VALUES (?, ?, ?, ?, ?)")
      .run(title, description || null, url, thumbnail_url || null, duration || null);
    return NextResponse.json({ id: result.lastInsertRowid, ...data });
  } catch (error) {
    return NextResponse.json({ error: "创建失败" }, { status: 500 });
  }
}
