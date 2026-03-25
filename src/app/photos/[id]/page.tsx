"use client";

import { use, useState, useEffect } from "react";
import Link from "next/link";

interface Photo {
  id: number;
  title: string;
  description: string | null;
  url: string;
  location: string | null;
  taken_at: string | null;
  created_at: string;
}

interface Comment {
  id: number;
  user_id: number;
  content: string;
  created_at: string;
  username: string;
}

export default function PhotoDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [photo, setPhoto] = useState<Photo | null>(null);
  const [comments, setComments] = useState<Comment[]>([]);
  const [likeCount, setLikeCount] = useState(0);
  const [liked, setLiked] = useState(false);
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [newComment, setNewComment] = useState("");

  useEffect(() => {
    // 获取照片详情
    fetch(`/api/photos/${id}`)
      .then((res) => res.json())
      .then(setPhoto);

    // 获取评论
    fetch(`/api/comments?target_type=photo&target_id=${id}`)
      .then((res) => res.json())
      .then(setComments);

    // 获取点赞数
    fetch(`/api/likes?target_type=photo&target_id=${id}`)
      .then((res) => res.json())
      .then((data) => setLikeCount(data.count || 0));

    // 检查登录状态和是否已点赞
    fetch("/api/auth/session", { credentials: "include" })
      .then((res) => res.json())
      .then((data) => {
        setIsLoggedIn(!!data?.user);
        if (data?.user) {
          fetch(`/api/likes/check?target_type=photo&target_id=${id}`, { credentials: "include" })
            .then((res) => res.json())
            .then((data) => setLiked(data.liked));
        }
      });
  }, [id]);

  const handleLike = async () => {
    if (!isLoggedIn) {
      alert("请先登录");
      return;
    }

    try {
      if (liked) {
        await fetch(`/api/likes?target_type=photo&target_id=${id}`, { method: "DELETE", credentials: "include" });
        setLiked(false);
        setLikeCount((c) => c - 1);
      } else {
        await fetch("/api/likes", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ target_type: "photo", target_id: parseInt(id) }),
          credentials: "include",
        });
        setLiked(true);
        setLikeCount((c) => c + 1);
      }
    } catch (error) {
      console.error("操作失败:", error);
    }
  };

  const handleComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isLoggedIn) {
      alert("请先登录");
      return;
    }
    if (!newComment.trim()) return;

    try {
      const res = await fetch("/api/comments", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ target_type: "photo", target_id: parseInt(id), content: newComment }),
        credentials: "include",
      });
      const comment = await res.json();
      setComments([...comments, comment]);
      setNewComment("");
    } catch (error) {
      console.error("评论失败:", error);
    }
  };

  if (!photo) {
    return <div className="min-h-screen flex items-center justify-center">加载中...</div>;
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8">
      <div className="max-w-4xl mx-auto px-4">
        <Link href="/photos" className="text-blue-500 hover:underline mb-4 inline-block">← 返回照片列表</Link>

        <div className="card overflow-hidden">
          <img src={photo.url} alt={photo.title} className="w-full max-h-[70vh] object-contain bg-black" />

          <div className="p-6">
            <h1 className="text-3xl font-bold mb-2">{photo.title}</h1>
            {photo.location && <p className="text-gray-500 mb-2">📍 {photo.location}</p>}
            {photo.description && <p className="text-gray-600 mt-4">{photo.description}</p>}

            {/* 点赞按钮 */}
            <div className="mt-6 flex items-center gap-4">
              <button
                onClick={handleLike}
                className={`flex items-center gap-2 px-4 py-2 rounded-full transition-colors ${
                  liked ? "bg-red-100 text-red-500" : "bg-gray-100 hover:bg-gray-200"
                }`}
              >
                {liked ? "❤️" : "🤍"} {likeCount}
              </button>
            </div>

            {/* 评论区 */}
            <div className="mt-8">
              <h2 className="text-xl font-bold mb-4">💬 评论 ({comments.length})</h2>

              {isLoggedIn ? (
                <form onSubmit={handleComment} className="mb-6">
                  <textarea
                    value={newComment}
                    onChange={(e) => setNewComment(e.target.value)}
                    className="input-field"
                    rows={3}
                    placeholder="写下你的评论..."
                  />
                  <button type="submit" className="btn-primary mt-2">发表评论</button>
                </form>
              ) : (
                <p className="text-gray-500 mb-6">
                  <Link href="/login" className="text-blue-500 hover:underline">登录</Link> 后可评论
                </p>
              )}

              <div className="space-y-4">
                {comments.map((comment) => (
                  <div key={comment.id} className="bg-gray-50 dark:bg-gray-800 p-4 rounded-lg">
                    <div className="flex items-center gap-2 mb-2">
                      <span className="font-semibold">{comment.username}</span>
                      <span className="text-sm text-gray-400">{new Date(comment.created_at).toLocaleString()}</span>
                    </div>
                    <p>{comment.content}</p>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
