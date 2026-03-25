import Link from "next/link";
import Image from "next/image";
import db, { initDatabase } from "@/lib/db";
import CTASection from "@/components/CTASection";

// 强制动态渲染，确保数据实时更新
export const dynamic = 'force-dynamic';

// 初始化数据库
initDatabase();

export default function HomePage() {
  // 获取统计数据
  const photoCount = db.prepare("SELECT COUNT(*) as count FROM photos").get() as { count: number };
  const routeCount = db.prepare("SELECT COUNT(*) as count FROM routes").get() as { count: number };
  const videoCount = db.prepare("SELECT COUNT(*) as count FROM videos").get() as { count: number };
  const storyCount = db.prepare("SELECT COUNT(*) as count FROM stories").get() as { count: number };

  // 获取最新内容
  const latestPhotos = db.prepare("SELECT * FROM photos ORDER BY created_at DESC LIMIT 4").all() as any[];
  const latestRoutes = db.prepare("SELECT * FROM routes ORDER BY created_at DESC LIMIT 3").all() as any[];

  return (
    <div className="min-h-screen">
      {/* Hero Section */}
      <section className="relative h-[60vh] bg-gradient-to-br from-blue-500 via-teal-400 to-green-400 flex items-center justify-center">
        <div className="absolute inset-0 bg-black/20"></div>
        <div className="relative z-10 text-center text-white px-4">
          <h1 className="text-5xl md:text-7xl font-bold mb-6 animate-fade-in">
            旅途记忆
          </h1>
          <p className="text-xl md:text-2xl mb-8 opacity-90">
            记录每一段美好旅程，分享旅途中的点点滴滴
          </p>
          <div className="flex flex-wrap justify-center gap-4">
            <Link href="/photos" className="btn-primary bg-white/90 text-blue-600 hover:bg-white">
              浏览照片
            </Link>
            <Link href="/routes" className="btn-primary bg-transparent border-2 border-white hover:bg-white/10">
              探索路线
            </Link>
          </div>
        </div>
      </section>

      {/* Stats Section */}
      <section className="py-16 bg-white dark:bg-gray-900">
        <div className="max-w-7xl mx-auto px-4">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
            <div className="text-center">
              <div className="text-4xl font-bold text-blue-500">{photoCount.count}</div>
              <div className="text-gray-500 mt-2">张照片</div>
            </div>
            <div className="text-center">
              <div className="text-4xl font-bold text-teal-500">{routeCount.count}</div>
              <div className="text-gray-500 mt-2">条路线</div>
            </div>
            <div className="text-center">
              <div className="text-4xl font-bold text-green-500">{videoCount.count}</div>
              <div className="text-gray-500 mt-2">个视频</div>
            </div>
            <div className="text-center">
              <div className="text-4xl font-bold text-purple-500">{storyCount.count}</div>
              <div className="text-gray-500 mt-2">篇故事</div>
            </div>
          </div>
        </div>
      </section>

      {/* Latest Photos Section */}
      <section className="py-16 bg-gray-50 dark:bg-gray-800">
        <div className="max-w-7xl mx-auto px-4">
          <div className="flex justify-between items-center mb-8">
            <h2 className="text-3xl font-bold text-gray-900 dark:text-white">
              最新照片
            </h2>
            <Link href="/photos" className="text-blue-500 hover:underline">
              查看全部 →
            </Link>
          </div>

          {latestPhotos.length > 0 ? (
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              {latestPhotos.map((photo) => (
                <Link key={photo.id} href={`/photos/${photo.id}`} className="card group">
                  <div className="aspect-square bg-gray-200 dark:bg-gray-700 overflow-hidden relative">
                    <Image
                      src={photo.url}
                      alt={photo.title}
                      fill
                      sizes="(max-width: 768px) 50vw, 25vw"
                      className="object-cover group-hover:scale-110 transition-transform duration-500"
                    />
                  </div>
                  <div className="p-4">
                    <h3 className="font-semibold text-gray-900 dark:text-white truncate">
                      {photo.title}
                    </h3>
                    {photo.location && (
                      <p className="text-sm text-gray-500 mt-1">{photo.location}</p>
                    )}
                  </div>
                </Link>
              ))}
            </div>
          ) : (
            <div className="text-center py-12 text-gray-500">
              <p className="text-6xl mb-4">📷</p>
              <p>暂无照片，请登录后台添加</p>
            </div>
          )}
        </div>
      </section>

      {/* Latest Routes Section */}
      <section className="py-16 bg-white dark:bg-gray-900">
        <div className="max-w-7xl mx-auto px-4">
          <div className="flex justify-between items-center mb-8">
            <h2 className="text-3xl font-bold text-gray-900 dark:text-white">
              热门路线
            </h2>
            <Link href="/routes" className="text-blue-500 hover:underline">
              查看全部 →
            </Link>
          </div>

          {latestRoutes.length > 0 ? (
            <div className="grid md:grid-cols-3 gap-6">
              {latestRoutes.map((route) => (
                <Link key={route.id} href={`/routes/${route.id}`} className="card group">
                  <div className="h-48 bg-gray-200 dark:bg-gray-700 overflow-hidden relative">
                    {route.cover_url ? (
                      <Image
                        src={route.cover_url}
                        alt={route.title}
                        fill
                        sizes="(max-width: 768px) 100vw, 33vw"
                        className="object-cover group-hover:scale-110 transition-transform duration-500"
                      />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center text-6xl">
                        🗺️
                      </div>
                    )}
                  </div>
                  <div className="p-6">
                    <h3 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">
                      {route.title}
                    </h3>
                    {route.description && (
                      <p className="text-gray-500 line-clamp-2">{route.description}</p>
                    )}
                    <div className="mt-4 flex items-center text-sm text-blue-500">
                      <span>📅 {route.days} 天行程</span>
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          ) : (
            <div className="text-center py-12 text-gray-500">
              <p className="text-6xl mb-4">🗺️</p>
              <p>暂无路线，请登录后台添加</p>
            </div>
          )}
        </div>
      </section>

      {/* CTA Section - 根据登录状态显示不同内容 */}
      <CTASection />
    </div>
  );
}
