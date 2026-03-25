import { NextRequest, NextResponse } from "next/server";
import db, { initDatabase } from "@/lib/db";

initDatabase();

export async function PUT(request: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  try {
    const { id } = await params;
    const data = await request.json();
    db.prepare("UPDATE videos SET title = ?, description = ?, url = ?, thumbnail_url = ?, duration = ? WHERE id = ?")
      .run(data.title, data.description || null, data.url, data.thumbnail_url || null, data.duration || null, id);
    return NextResponse.json({ success: true });
  } catch (error) {
    return NextResponse.json({ error: "更新失败" }, { status: 500 });
  }
}

export async function DELETE(request: NextRequest, { params }: { params: Promise<{ id: string }> }) {
  try {
    const { id } = await params;
    db.prepare("DELETE FROM videos WHERE id = ?").run(id);
    return NextResponse.json({ success: true });
  } catch (error) {
    return NextResponse.json({ error: "删除失败" }, { status: 500 });
  }
}
