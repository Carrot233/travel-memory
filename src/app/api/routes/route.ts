import { NextResponse } from "next/server";
import db, { initDatabase } from "@/lib/db";

initDatabase();

export async function GET() {
  const routes = db.prepare("SELECT * FROM routes ORDER BY created_at DESC").all();
  return NextResponse.json(routes);
}

export async function POST(request: Request) {
  try {
    const data = await request.json();
    const { title, description, cover_url, days, spots } = data;

    if (!title) {
      return NextResponse.json({ error: "标题不能为空" }, { status: 400 });
    }

    const result = db
      .prepare("INSERT INTO routes (title, description, cover_url, days) VALUES (?, ?, ?, ?)")
      .run(title, description || null, cover_url || null, days || 1);

    const routeId = result.lastInsertRowid;

    // 保存景点
    if (spots && Array.isArray(spots) && spots.length > 0) {
      const insertSpot = db.prepare(
        "INSERT INTO route_spots (route_id, name, description, latitude, longitude, day_number, order_index, image_url) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
      );
      spots.forEach((spot: any) => {
        insertSpot.run(
          routeId,
          spot.name,
          spot.description || null,
          spot.latitude || null,
          spot.longitude || null,
          spot.day_number || 1,
          spot.order_index || 0,
          spot.image_url || null
        );
      });
    }

    return NextResponse.json({ id: routeId, ...data });
  } catch (error) {
    console.error("创建路线失败:", error);
    return NextResponse.json({ error: "创建失败" }, { status: 500 });
  }
}
