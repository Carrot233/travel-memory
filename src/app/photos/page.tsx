import Link from "next/link";
import Image from "next/image";
import db, { initDatabase } from "@/lib/db";

initDatabase();

// 强制动态渲染
export const dynamic = 'force-dynamic';

export default function PhotosPage() {
  const photos = db.prepare("SELECT * FROM photos ORDER BY created_at DESC").all() as any[];

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8">
      <div className="max-w-7xl mx-auto px-4">
        <h1 className="text-4xl font-bold text-gray-900 dark:text-white mb-8">
          📷 照片画廊
        </h1>

        {photos.length > 0 ? (
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            {photos.map((photo) => (
              <Link key={photo.id} href={`/photos/${photo.id}`} className="card group">
                <div className="aspect-square overflow-hidden relative">
                  <Image
                    src={photo.url}
                    alt={photo.title}
                    fill
                    sizes="(max-width: 768px) 50vw, (max-width: 1024px) 33vw, 25vw"
                    className="object-cover group-hover:scale-110 transition-transform duration-500"
                  />
                </div>
                <div className="p-3">
                  <h3 className="font-semibold truncate">{photo.title}</h3>
                  {photo.location && (
                    <p className="text-sm text-gray-500">📍 {photo.location}</p>
                  )}
                </div>
              </Link>
            ))}
          </div>
        ) : (
          <div className="text-center py-20 text-gray-500">
            <p className="text-6xl mb-4">📷</p>
            <p>暂无照片</p>
          </div>
        )}
      </div>
    </div>
  );
}
