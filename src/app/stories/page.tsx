import Link from "next/link";
import Image from "next/image";
import db, { initDatabase } from "@/lib/db";

initDatabase();

// 强制动态渲染
export const dynamic = 'force-dynamic';

export default function StoriesPage() {
  const stories = db.prepare("SELECT * FROM stories ORDER BY created_at DESC").all() as any[];

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8">
      <div className="max-w-4xl mx-auto px-4">
        <h1 className="text-4xl font-bold text-gray-900 dark:text-white mb-8">
          📝 旅行故事
        </h1>

        {stories.length > 0 ? (
          <div className="space-y-6">
            {stories.map((story) => (
              <Link key={story.id} href={`/stories/${story.id}`} className="card p-6 block group hover:shadow-xl transition-shadow">
                <div className="flex gap-4">
                  {story.cover_url ? (
                    <div className="w-32 h-32 relative rounded-lg overflow-hidden shrink-0">
                      <Image 
                        src={story.cover_url} 
                        alt={story.title} 
                        fill
                        sizes="8rem"
                        className="object-cover" 
                      />
                    </div>
                  ) : (
                    <div className="w-32 h-32 bg-gradient-to-br from-purple-400 to-pink-400 rounded-lg flex items-center justify-center text-4xl shrink-0">📝</div>
                  )}
                  <div className="flex-1">
                    <h2 className="text-2xl font-bold group-hover:text-blue-500 transition-colors">{story.title}</h2>
                    {story.location && <p className="text-gray-500 mt-1">📍 {story.location}</p>}
                    <p className="text-gray-600 mt-2 line-clamp-2">{story.content}</p>
                    <p className="text-sm text-gray-400 mt-2">{new Date(story.created_at).toLocaleDateString()}</p>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        ) : (
          <div className="text-center py-20 text-gray-500">
            <p className="text-6xl mb-4">📝</p>
            <p>暂无故事</p>
          </div>
        )}
      </div>
    </div>
  );
}
