import { NextRequest, NextResponse } from "next/server";
import db, { initDatabase } from "@/lib/db";

initDatabase();

export async function PUT(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const { id } = await params;
    const data = await request.json();
    const { name, description, image_url, latitude, longitude, day_number, order_index } = data;

    db.prepare(
      "UPDATE route_spots SET name = ?, description = ?, image_url = ?, latitude = ?, longitude = ?, day_number = ?, order_index = ? WHERE id = ?"
    ).run(
      name,
      description || null,
      image_url || null,
      latitude || null,
      longitude || null,
      day_number || 1,
      order_index || 0,
      parseInt(id)
    );

    return NextResponse.json({ success: true });
  } catch (error) {
    console.error("更新景点失败:", error);
    return NextResponse.json({ error: "更新失败" }, { status: 500 });
  }
}

export async function DELETE(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const { id } = await params;
    db.prepare("DELETE FROM route_spots WHERE id = ?").run(parseInt(id));
    return NextResponse.json({ success: true });
  } catch (error) {
    console.error("删除景点失败:", error);
    return NextResponse.json({ error: "删除失败" }, { status: 500 });
  }
}
