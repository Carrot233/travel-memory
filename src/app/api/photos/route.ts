import { NextResponse } from "next/server";
import db, { initDatabase } from "@/lib/db";

initDatabase();

export async function GET(request: Request) {
  // 支持 route_id 查询参数
  const { searchParams } = new URL(request.url || "http://localhost");
  const routeId = searchParams.get("route_id");
  
  let query = "SELECT * FROM photos";
  let params: any[] = [];
  
  if (routeId) {
    query += " WHERE route_id = ?";
    params.push(routeId);
  }
  
  query += " ORDER BY created_at DESC";
  
  const photos = routeId 
    ? db.prepare(query).all(...params)
    : db.prepare(query).all();
    
  return NextResponse.json(photos);
}

export async function POST(request: Request) {
  try {
    const data = await request.json();
    const { title, description, url, location, route_id, spot_id, taken_at } = data;

    if (!title || !url) {
      return NextResponse.json({ error: "标题和URL不能为空" }, { status: 400 });
    }

    const result = db
      .prepare("INSERT INTO photos (title, description, url, location, route_id, spot_id, taken_at) VALUES (?, ?, ?, ?, ?, ?, ?)")
      .run(title, description || null, url, location || null, route_id || null, spot_id || null, taken_at || null);

    return NextResponse.json({ id: result.lastInsertRowid, ...data });
  } catch (error) {
    console.error("创建照片失败:", error);
    return NextResponse.json({ error: "创建失败" }, { status: 500 });
  }
}
