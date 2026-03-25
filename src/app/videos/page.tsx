import Link from "next/link";
import db, { initDatabase } from "@/lib/db";

initDatabase();

// 强制动态渲染
export const dynamic = 'force-dynamic';

export default function VideosPage() {
  const videos = db.prepare("SELECT * FROM videos ORDER BY created_at DESC").all() as any[];

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8">
      <div className="max-w-7xl mx-auto px-4">
        <h1 className="text-4xl font-bold text-gray-900 dark:text-white mb-8">
          🎬 旅途视频
        </h1>

        {videos.length > 0 ? (
          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
            {videos.map((video) => (
              <div key={video.id} className="card overflow-hidden">
                <div className="aspect-video bg-black">
                  <video
                    src={video.url}
                    poster={video.thumbnail_url}
                    controls
                    className="w-full h-full"
                  />
                </div>
                <div className="p-4">
                  <h3 className="font-semibold text-lg">{video.title}</h3>
                  {video.description && (
                    <p className="text-gray-500 mt-1 line-clamp-2">{video.description}</p>
                  )}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-20 text-gray-500">
            <p className="text-6xl mb-4">🎬</p>
            <p>暂无视频</p>
          </div>
        )}
      </div>
    </div>
  );
}
