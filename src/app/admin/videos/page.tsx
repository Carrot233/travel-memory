"use client";

import { useEffect, useState, useRef } from "react";
import Link from "next/link";
import Image from "next/image";

interface Video {
  id: number;
  title: string;
  description: string | null;
  url: string;
  thumbnail_url: string | null;
  duration: number | null;
  created_at: string;
}

export default function AdminVideosPage() {
  const [videos, setVideos] = useState<Video[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingVideo, setEditingVideo] = useState<Video | null>(null);
  const [uploadingVideo, setUploadingVideo] = useState(false);
  const [uploadingThumb, setUploadingThumb] = useState(false);
  const videoInputRef = useRef<HTMLInputElement>(null);
  const thumbInputRef = useRef<HTMLInputElement>(null);
  const [formData, setFormData] = useState({
    title: "",
    description: "",
    url: "",
    thumbnail_url: "",
  });

  const fetchVideos = async () => {
    try {
      const res = await fetch("/api/videos");
      const data = await res.json();
      setVideos(data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchVideos();
  }, []);

  // 上传视频文件
  const handleVideoUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setUploadingVideo(true);
    try {
      const uploadFormData = new FormData();
      uploadFormData.append("file", file);
      uploadFormData.append("type", "videos");

      const res = await fetch("/api/upload", {
        method: "POST",
        body: uploadFormData,
      });

      const data = await res.json();
      if (res.ok && data.url) {
        setFormData({ ...formData, url: data.url });
      } else {
        alert(data.error || "上传失败");
      }
    } catch (error) {
      console.error("上传失败:", error);
      alert("上传失败，请重试");
    } finally {
      setUploadingVideo(false);
    }
  };

  // 上传封面图
  const handleThumbUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setUploadingThumb(true);
    try {
      const uploadFormData = new FormData();
      uploadFormData.append("file", file);
      uploadFormData.append("type", "thumbnails");

      const res = await fetch("/api/upload", {
        method: "POST",
        body: uploadFormData,
      });

      const data = await res.json();
      if (res.ok && data.url) {
        setFormData({ ...formData, thumbnail_url: data.url });
      } else {
        alert(data.error || "上传失败");
      }
    } catch (error) {
      console.error("上传失败:", error);
      alert("上传失败，请重试");
    } finally {
      setUploadingThumb(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!formData.url) {
      alert("请上传视频或输入视频URL");
      return;
    }

    const url = editingVideo ? `/api/videos/${editingVideo.id}` : "/api/videos";
    const method = editingVideo ? "PUT" : "POST";

    try {
      const res = await fetch(url, {
        method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(formData),
      });
      if (res.ok) {
        setShowForm(false);
        setEditingVideo(null);
        setFormData({ title: "", description: "", url: "", thumbnail_url: "" });
        fetchVideos();
      }
    } catch (error) {
      console.error("保存失败:", error);
    }
  };

  const handleEdit = (video: Video) => {
    setEditingVideo(video);
    setFormData({
      title: video.title,
      description: video.description || "",
      url: video.url,
      thumbnail_url: video.thumbnail_url || "",
    });
    setShowForm(true);
  };

  const handleDelete = async (id: number) => {
    if (!confirm("确定要删除这个视频吗？")) return;
    try {
      await fetch(`/api/videos/${id}`, { method: "DELETE" });
      fetchVideos();
    } catch (error) {
      console.error("删除失败:", error);
    }
  };

  if (loading) return <div className="min-h-screen flex items-center justify-center">加载中...</div>;

  return (
    <div className="min-h-screen bg-gray-100 dark:bg-gray-900 py-8">
      <div className="max-w-7xl mx-auto px-4">
        <div className="flex items-center justify-between mb-8">
          <div>
            <Link href="/admin" className="text-blue-500 hover:underline mb-2 inline-block">← 返回后台</Link>
            <h1 className="text-3xl font-bold text-gray-900 dark:text-white">视频管理</h1>
          </div>
          <button onClick={() => { setEditingVideo(null); setFormData({ title: "", description: "", url: "", thumbnail_url: "" }); setShowForm(true); }} className="btn-primary">+ 添加视频</button>
        </div>

        {showForm && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div className="card p-6 w-full max-w-md max-h-[90vh] overflow-y-auto">
              <h2 className="text-xl font-bold mb-4">{editingVideo ? "编辑视频" : "添加视频"}</h2>
              <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium mb-1">标题 *</label>
                  <input type="text" value={formData.title} onChange={(e) => setFormData({ ...formData, title: e.target.value })} className="input-field" required />
                </div>
                
                {/* 视频上传区域 */}
                <div>
                  <label className="block text-sm font-medium mb-2">视频文件 *</label>
                  
                  <input
                    ref={videoInputRef}
                    type="file"
                    accept="video/mp4,video/webm,video/ogg"
                    onChange={handleVideoUpload}
                    className="hidden"
                  />
                  <button
                    type="button"
                    onClick={() => videoInputRef.current?.click()}
                    disabled={uploadingVideo}
                    className="w-full py-3 px-4 border-2 border-dashed border-gray-300 rounded-lg hover:border-blue-500 transition-colors disabled:opacity-50 text-center"
                  >
                    {uploadingVideo ? "⏳ 上传中..." : "📁 点击上传本地视频"}
                  </button>
                  
                  {/* 显示已上传的视频 */}
                  {formData.url && (
                    <div className="mt-3 p-2 bg-gray-100 dark:bg-gray-700 rounded-lg flex items-center justify-between">
                      <span className="text-sm truncate flex-1">
                        {formData.url.startsWith("/api/uploads/") || formData.url.startsWith("/uploads/") ? "✅ " : "🔗 "}
                        {formData.url.split("/").pop()}
                      </span>
                      <button
                        type="button"
                        onClick={() => setFormData({ ...formData, url: "" })}
                        className="text-red-500 hover:text-red-600 ml-2 text-sm"
                      >
                        移除
                      </button>
                    </div>
                  )}
                  
                  {/* 分隔线 */}
                  <div className="flex items-center my-3">
                    <div className="flex-1 border-t border-gray-300"></div>
                    <span className="px-3 text-gray-400 text-sm">或</span>
                    <div className="flex-1 border-t border-gray-300"></div>
                  </div>
                  
                  {/* 输入 URL */}
                  <input
                    type="url"
                    value={formData.url.startsWith("/api/uploads/") || formData.url.startsWith("/uploads/") ? "" : formData.url}
                    onChange={(e) => setFormData({ ...formData, url: e.target.value })}
                    className="input-field"
                    placeholder="输入视频 URL（如 https://example.com/video.mp4）"
                    disabled={formData.url.startsWith("/api/uploads/") || formData.url.startsWith("/uploads/")}
                  />
                  {(formData.url.startsWith("/api/uploads/") || formData.url.startsWith("/uploads/")) && (
                    <p className="text-xs text-gray-400 mt-1">已上传本地视频，清空后可输入URL</p>
                  )}
                </div>
                
                {/* 封面图上传 */}
                <div>
                  <label className="block text-sm font-medium mb-2">封面图</label>
                  
                  <input
                    ref={thumbInputRef}
                    type="file"
                    accept="image/jpeg,image/png,image/gif,image/webp"
                    onChange={handleThumbUpload}
                    className="hidden"
                  />
                  <button
                    type="button"
                    onClick={() => thumbInputRef.current?.click()}
                    disabled={uploadingThumb}
                    className="w-full py-3 px-4 border-2 border-dashed border-gray-300 rounded-lg hover:border-blue-500 transition-colors disabled:opacity-50 text-center"
                  >
                    {uploadingThumb ? "⏳ 上传中..." : "🖼️ 点击上传封面图"}
                  </button>
                  
                  {formData.thumbnail_url && (
                    <div className="mt-3 relative h-32">
                      <Image
                        src={formData.thumbnail_url}
                        alt="封面预览"
                        fill
                        sizes="100vw"
                        className="object-cover rounded-lg border"
                      />
                      <button
                        type="button"
                        onClick={() => setFormData({ ...formData, thumbnail_url: "" })}
                        className="absolute top-2 right-2 bg-red-500 text-white rounded-full w-6 h-6 flex items-center justify-center hover:bg-red-600"
                      >
                        ×
                      </button>
                    </div>
                  )}
                  
                  {/* 分隔线 */}
                  <div className="flex items-center my-3">
                    <div className="flex-1 border-t border-gray-300"></div>
                    <span className="px-3 text-gray-400 text-sm">或</span>
                    <div className="flex-1 border-t border-gray-300"></div>
                  </div>
                  
                  <input
                    type="url"
                    value={formData.thumbnail_url.startsWith("/api/uploads/") || formData.thumbnail_url.startsWith("/uploads/") ? "" : formData.thumbnail_url}
                    onChange={(e) => setFormData({ ...formData, thumbnail_url: e.target.value })}
                    className="input-field"
                    placeholder="输入封面图 URL"
                    disabled={formData.thumbnail_url.startsWith("/api/uploads/") || formData.thumbnail_url.startsWith("/uploads/")}
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-medium mb-1">描述</label>
                  <textarea value={formData.description} onChange={(e) => setFormData({ ...formData, description: e.target.value })} className="input-field" rows={3} />
                </div>
                <div className="flex gap-4">
                  <button type="submit" className="btn-primary flex-1" disabled={!formData.url}>保存</button>
                  <button type="button" onClick={() => setShowForm(false)} className="px-6 py-2 border rounded-full">取消</button>
                </div>
              </form>
            </div>
          </div>
        )}

        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          {videos.map((video) => (
            <div key={video.id} className="card group">
              <div className="aspect-video relative overflow-hidden bg-gray-200 dark:bg-gray-700">
                {video.thumbnail_url ? (
                  <Image src={video.thumbnail_url} alt={video.title} fill sizes="(max-width: 768px) 100vw, 50vw" className="object-cover" />
                ) : (
                  <div className="w-full h-full flex items-center justify-center text-4xl">🎬</div>
                )}
                <div className="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-2">
                  <button onClick={() => handleEdit(video)} className="px-4 py-2 bg-white text-gray-900 rounded-full text-sm">编辑</button>
                  <button onClick={() => handleDelete(video.id)} className="px-4 py-2 bg-red-500 text-white rounded-full text-sm">删除</button>
                </div>
              </div>
              <div className="p-4">
                <h3 className="font-semibold">{video.title}</h3>
              </div>
            </div>
          ))}
        </div>

        {videos.length === 0 && (
          <div className="text-center py-16 text-gray-500">
            <p className="text-6xl mb-4">🎬</p>
            <p>暂无视频，点击上方按钮添加</p>
          </div>
        )}
      </div>
    </div>
  );
}
