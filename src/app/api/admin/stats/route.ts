import { NextResponse } from "next/server";
import db, { initDatabase } from "@/lib/db";

initDatabase();

export async function GET() {
  const stats = {
    photos: (db.prepare("SELECT COUNT(*) as count FROM photos").get() as { count: number }).count,
    routes: (db.prepare("SELECT COUNT(*) as count FROM routes").get() as { count: number }).count,
    videos: (db.prepare("SELECT COUNT(*) as count FROM videos").get() as { count: number }).count,
    stories: (db.prepare("SELECT COUNT(*) as count FROM stories").get() as { count: number }).count,
  };

  return NextResponse.json(stats);
}
