import { NextRequest, NextResponse } from "next/server";
import db, { initDatabase } from "@/lib/db";

initDatabase();

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  const { id } = await params;
  const photo = db.prepare("SELECT * FROM photos WHERE id = ?").get(id);

  if (!photo) {
    return NextResponse.json({ error: "照片不存在" }, { status: 404 });
  }

  return NextResponse.json(photo);
}

export async function PUT(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const { id } = await params;
    const data = await request.json();
    const { title, description, url, location, taken_at } = data;

    db.prepare(
      "UPDATE photos SET title = ?, description = ?, url = ?, location = ?, taken_at = ? WHERE id = ?"
    ).run(title, description || null, url, location || null, taken_at || null, id);

    return NextResponse.json({ success: true });
  } catch (error) {
    console.error("更新照片失败:", error);
    return NextResponse.json({ error: "更新失败" }, { status: 500 });
  }
}

export async function DELETE(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const { id } = await params;
    db.prepare("DELETE FROM photos WHERE id = ?").run(id);
    return NextResponse.json({ success: true });
  } catch (error) {
    console.error("删除照片失败:", error);
    return NextResponse.json({ error: "删除失败" }, { status: 500 });
  }
}
