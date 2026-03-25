import { NextRequest, NextResponse } from "next/server";
import db, { initDatabase } from "@/lib/db";

initDatabase();

export async function POST(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const { id } = await params;
    const data = await request.json();
    const { name, description, image_url, latitude, longitude, day_number, order_index } = data;

    if (!name) {
      return NextResponse.json({ error: "景点名称不能为空" }, { status: 400 });
    }

    const result = db
      .prepare(
        "INSERT INTO route_spots (route_id, name, description, image_url, latitude, longitude, day_number, order_index) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
      )
      .run(
        parseInt(id),
        name,
        description || null,
        image_url || null,
        latitude || null,
        longitude || null,
        day_number || 1,
        order_index || 0
      );

    return NextResponse.json({ id: result.lastInsertRowid, ...data, route_id: parseInt(id) });
  } catch (error) {
    console.error("创建景点失败:", error);
    return NextResponse.json({ error: "创建失败" }, { status: 500 });
  }
}
