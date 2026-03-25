"use client";

import { use, useState, useEffect } from "react";
import Link from "next/link";
import Image from "next/image";

interface Story {
  id: number;
  title: string;
  content: string;
  cover_url: string | null;
  location: string | null;
  photo_ids: string | null;
  route_ids: string | null;
  created_at: string;
}

interface Photo {
  id: number;
  title: string;
  url: string;
}

interface Route {
  id: number;
  title: string;
  cover_url: string | null;
}

interface Comment {
  id: number;
  user_id: number;
  content: string;
  created_at: string;
  username: string;
}

export default function StoryDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [story, setStory] = useState<Story | null>(null);
  const [photos, setPhotos] = useState<Photo[]>([]);
  const [routes, setRoutes] = useState<Route[]>([]);
  const [comments, setComments] = useState<Comment[]>([]);
  const [likeCount, setLikeCount] = useState(0);
  const [liked, setLiked] = useState(false);
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [newComment, setNewComment] = useState("");

  useEffect(() => {
    fetch(`/api/stories/${id}`)
      .then((res) => res.json())
      .then((data) => {
        setStory(data);
        // 解析关联的照片和路线
        const photoIds = data.photo_ids ? JSON.parse(data.photo_ids) : [];
        const routeIds = data.route_ids ? JSON.parse(data.route_ids) : [];
        
        // 获取关联的照片
        if (photoIds.length > 0) {
          fetch("/api/photos")
            .then(res => res.json())
            .then(allPhotos => {
              setPhotos(allPhotos.filter((p: Photo) => photoIds.includes(p.id)));
            });
        }
        
        // 获取关联的路线
        if (routeIds.length > 0) {
          fetch("/api/routes")
            .then(res => res.json())
            .then(allRoutes => {
              setRoutes(allRoutes.filter((r: Route) => routeIds.includes(r.id)));
            });
        }
      });
    fetch(`/api/comments?target_type=story&target_id=${id}`)
      .then((res) => res.json())
      .then(setComments);
    fetch(`/api/likes?target_type=story&target_id=${id}`)
      .then((res) => res.json())
      .then((data) => setLikeCount(data.count || 0));
    fetch("/api/auth/session", { credentials: "include" })
      .then((res) => res.json())
      .then((data) => {
        setIsLoggedIn(!!data?.user);
        if (data?.user) {
          fetch(`/api/likes/check?target_type=story&target_id=${id}`, { credentials: "include" })
            .then((res) => res.json())
            .then((data) => setLiked(data.liked));
        }
      });
  }, [id]);

  const handleLike = async () => {
    if (!isLoggedIn) { alert("请先登录"); return; }
    try {
      if (liked) {
        await fetch(`/api/likes?target_type=story&target_id=${id}`, { method: "DELETE", credentials: "include" });
        setLiked(false); setLikeCount((c) => c - 1);
      } else {
        await fetch("/api/likes", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ target_type: "story", target_id: parseInt(id) }),
          credentials: "include",
        });
        setLiked(true); setLikeCount((c) => c + 1);
      }
    } catch (error) { console.error("操作失败:", error); }
  };

  const handleComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isLoggedIn) { alert("请先登录"); return; }
    if (!newComment.trim()) return;
    try {
      const res = await fetch("/api/comments", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ target_type: "story", target_id: parseInt(id), content: newComment }),
        credentials: "include",
      });
      const comment = await res.json();
      setComments([comment, ...comments]);
      setNewComment("");
    } catch (error) { console.error("评论失败:", error); }
  };

  if (!story) return <div className="min-h-screen flex items-center justify-center">加载中...</div>;

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8">
      <div className="max-w-4xl mx-auto px-4">
        <Link href="/stories" className="text-blue-500 hover:underline mb-4 inline-block">← 返回故事列表</Link>

        <article className="card overflow-hidden">
          {story.cover_url && (
            <img src={story.cover_url} alt={story.title} className="w-full h-64 object-cover" />
          )}
          <div className="p-6">
            <h1 className="text-3xl font-bold mb-2">{story.title}</h1>
            <div className="flex items-center gap-4 text-gray-500 mb-6">
              {story.location && <span>📍 {story.location}</span>}
              <span>📅 {new Date(story.created_at).toLocaleDateString()}</span>
            </div>

            <div className="prose dark:prose-invert max-w-none">
              {story.content.split("\n").map((p, i) => (
                <p key={i} className="mb-4">{p}</p>
              ))}
            </div>

            {/* 关联的照片 */}
            {photos.length > 0 && (
              <div className="mt-8 pt-6 border-t">
                <h2 className="text-xl font-bold mb-4">📷 相关照片</h2>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                  {photos.map((photo) => (
                    <div key={photo.id} className="aspect-square rounded-lg overflow-hidden bg-gray-100 relative">
                      <Image src={photo.url} alt={photo.title} fill sizes="(max-width: 768px) 50vw, 25vw" className="object-cover hover:scale-105 transition-transform" />
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* 关联的路线 */}
            {routes.length > 0 && (
              <div className="mt-8 pt-6 border-t">
                <h2 className="text-xl font-bold mb-4">🗺️ 相关路线</h2>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {routes.map((route) => (
                    <Link key={route.id} href={`/routes/${route.id}`} className="block p-4 bg-gray-50 dark:bg-gray-800 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors">
                      <div className="flex items-center gap-4">
                        <div className="w-16 h-16 rounded-lg bg-gradient-to-br from-blue-400 to-teal-400 flex items-center justify-center text-2xl">
                          🗺️
                        </div>
                        <div>
                          <h3 className="font-semibold">{route.title}</h3>
                          <p className="text-sm text-gray-500">点击查看路线详情</p>
                        </div>
                      </div>
                    </Link>
                  ))}
                </div>
              </div>
            )}

            <div className="mt-8 pt-6 border-t">
              <button onClick={handleLike} className={`flex items-center gap-2 px-4 py-2 rounded-full ${liked ? "bg-red-100 text-red-500" : "bg-gray-100 hover:bg-gray-200"}`}>
                {liked ? "❤️" : "🤍"} {likeCount}
              </button>
            </div>

            <div className="mt-8">
              <h2 className="text-xl font-bold mb-4">💬 评论 ({comments.length})</h2>
              {isLoggedIn ? (
                <form onSubmit={handleComment} className="mb-6">
                  <textarea value={newComment} onChange={(e) => setNewComment(e.target.value)} className="input-field" rows={3} placeholder="写下你的评论..." />
                  <button type="submit" className="btn-primary mt-2">发表评论</button>
                </form>
              ) : (
                <p className="text-gray-500 mb-6"><Link href="/login" className="text-blue-500 hover:underline">登录</Link> 后可评论</p>
              )}
              <div className="space-y-4">
                {comments.map((c) => (
                  <div key={c.id} className="bg-gray-50 dark:bg-gray-800 p-4 rounded-lg">
                    <div className="flex items-center gap-2 mb-2">
                      <span className="font-semibold">{c.username}</span>
                      <span className="text-sm text-gray-400">{new Date(c.created_at).toLocaleString()}</span>
                    </div>
                    <p>{c.content}</p>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </article>
      </div>
    </div>
  );
}
