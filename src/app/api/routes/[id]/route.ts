import { NextRequest, NextResponse } from "next/server";
import db, { initDatabase } from "@/lib/db";

initDatabase();

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  const { id } = await params;
  const route = db.prepare("SELECT * FROM routes WHERE id = ?").get(id);

  if (!route) {
    return NextResponse.json({ error: "路线不存在" }, { status: 404 });
  }

  const spots = db.prepare("SELECT * FROM route_spots WHERE route_id = ? ORDER BY day_number, order_index").all(id);
  return NextResponse.json({ ...route, spots });
}

export async function PUT(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const { id } = await params;
    const data = await request.json();
    const { title, description, cover_url, days } = data;

    db.prepare(
      "UPDATE routes SET title = ?, description = ?, cover_url = ?, days = ? WHERE id = ?"
    ).run(title, description || null, cover_url || null, days || 1, id);

    return NextResponse.json({ success: true });
  } catch (error) {
    console.error("更新路线失败:", error);
    return NextResponse.json({ error: "更新失败" }, { status: 500 });
  }
}

export async function DELETE(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const { id } = await params;
    db.prepare("DELETE FROM routes WHERE id = ?").run(id);
    return NextResponse.json({ success: true });
  } catch (error) {
    console.error("删除路线失败:", error);
    return NextResponse.json({ error: "删除失败" }, { status: 500 });
  }
}
