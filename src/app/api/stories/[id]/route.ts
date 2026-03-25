import { NextRequest, NextResponse } from "next/server";
import db, { initDatabase } from "@/lib/db";

initDatabase();

// 获取单个故事
export async function GET(request: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const story = db.prepare("SELECT * FROM stories WHERE id = ?").get(id);
  if (!story) return NextResponse.json({ error: "故事不存在" }, { status: 404 });
  return NextResponse.json(story);
}

// 更新故事
export async function PUT(request: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  try {
    const { id } = await params;
    const data = await request.json();
    const { title, content, cover_url, location, photo_ids, route_ids } = data;
    
    if (!title || !content) return NextResponse.json({ error: "标题和内容不能为空" }, { status: 400 });
    
    db.prepare(`UPDATE stories SET title = ?, content = ?, cover_url = ?, location = ?, photo_ids = ?, route_ids = ? WHERE id = ?`)
      .run(title, content, cover_url || null, location || null, photo_ids ? JSON.stringify(photo_ids) : null, route_ids ? JSON.stringify(route_ids) : null, id);
    
    return NextResponse.json({ id, ...data });
  } catch (error) {
    return NextResponse.json({ error: "更新失败" }, { status: 500 });
  }
}

// 删除故事
export async function DELETE(request: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  try {
    const { id } = await params;
    db.prepare("DELETE FROM stories WHERE id = ?").run(id);
    return NextResponse.json({ success: true });
  } catch (error) {
    return NextResponse.json({ error: "删除失败" }, { status: 500 });
  }
}
