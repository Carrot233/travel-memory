import Link from "next/link";
import Image from "next/image";
import db, { initDatabase } from "@/lib/db";

initDatabase();

// 强制动态渲染
export const dynamic = 'force-dynamic';

export default function RoutesPage() {
  const routes = db.prepare("SELECT * FROM routes ORDER BY created_at DESC").all() as any[];

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8">
      <div className="max-w-7xl mx-auto px-4">
        <h1 className="text-4xl font-bold text-gray-900 dark:text-white mb-8">
          🗺️ 旅游路线
        </h1>

        {routes.length > 0 ? (
          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
            {routes.map((route) => (
              <Link key={route.id} href={`/routes/${route.id}`} className="card group">
                <div className="h-48 overflow-hidden relative">
                  {route.cover_url ? (
                    <Image
                      src={route.cover_url}
                      alt={route.title}
                      fill
                      sizes="(max-width: 768px) 100vw, (max-width: 1024px) 50vw, 33vw"
                      className="object-cover group-hover:scale-110 transition-transform duration-500"
                    />
                  ) : (
                    <div className="w-full h-full bg-gradient-to-br from-teal-400 to-blue-500 flex items-center justify-center text-4xl">
                      🗺️
                    </div>
                  )}
                </div>
                <div className="p-4">
                  <h3 className="text-xl font-semibold">{route.title}</h3>
                  <p className="text-gray-500 mt-1">📅 {route.days} 天行程</p>
                  {route.description && (
                    <p className="text-gray-400 mt-2 line-clamp-2">{route.description}</p>
                  )}
                </div>
              </Link>
            ))}
          </div>
        ) : (
          <div className="text-center py-20 text-gray-500">
            <p className="text-6xl mb-4">🗺️</p>
            <p>暂无路线</p>
          </div>
        )}
      </div>
    </div>
  );
}
