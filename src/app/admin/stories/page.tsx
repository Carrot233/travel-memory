"use client";

import { useEffect, useState, useRef } from "react";
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

export default function AdminStoriesPage() {
  const [stories, setStories] = useState<Story[]>([]);
  const [photos, setPhotos] = useState<Photo[]>([]);
  const [routes, setRoutes] = useState<Route[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingStory, setEditingStory] = useState<Story | null>(null);
  const [formData, setFormData] = useState({ 
    title: "", 
    content: "", 
    cover_url: "", 
    location: "",
    photo_ids: [] as number[],
    route_ids: [] as number[]
  });

  const fetchStories = async () => {
    try {
      const res = await fetch("/api/stories");
      const data = await res.json();
      setStories(data);
    } finally {
      setLoading(false);
    }
  };

  const fetchPhotosAndRoutes = async () => {
    try {
      const [photosRes, routesRes] = await Promise.all([
        fetch("/api/photos"),
        fetch("/api/routes")
      ]);
      const photosData = await photosRes.json();
      const routesData = await routesRes.json();
      setPhotos(photosData);
      setRoutes(routesData);
    } catch (error) {
      console.error("获取数据失败:", error);
    }
  };

  useEffect(() => { 
    fetchStories(); 
    fetchPhotosAndRoutes();
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const url = editingStory ? `/api/stories/${editingStory.id}` : "/api/stories";
    const method = editingStory ? "PUT" : "POST";
    try {
      const res = await fetch(url, { 
        method, 
        headers: { "Content-Type": "application/json" }, 
        body: JSON.stringify(formData) 
      });
      if (res.ok) { 
        setShowForm(false); 
        setEditingStory(null); 
        setFormData({ title: "", content: "", cover_url: "", location: "", photo_ids: [], route_ids: [] }); 
        fetchStories(); 
      }
    } catch (error) { console.error("保存失败:", error); }
  };

  const handleEdit = (story: Story) => {
    setEditingStory(story);
    let photoIds: number[] = [];
    let routeIds: number[] = [];
    try {
      if (story.photo_ids) photoIds = JSON.parse(story.photo_ids);
      if (story.route_ids) routeIds = JSON.parse(story.route_ids);
    } catch {}
    setFormData({ 
      title: story.title, 
      content: story.content, 
      cover_url: story.cover_url || "", 
      location: story.location || "",
      photo_ids: photoIds,
      route_ids: routeIds
    });
    setShowForm(true);
  };

  const handleDelete = async (id: number) => {
    if (!confirm("确定要删除这个故事吗？")) return;
    try { await fetch(`/api/stories/${id}`, { method: "DELETE" }); fetchStories(); } catch (error) { console.error("删除失败:", error); }
  };

  const togglePhoto = (photoId: number) => {
    setFormData(prev => ({
      ...prev,
      photo_ids: prev.photo_ids.includes(photoId)
        ? prev.photo_ids.filter(id => id !== photoId)
        : [...prev.photo_ids, photoId]
    }));
  };

  const toggleRoute = (routeId: number) => {
    setFormData(prev => ({
      ...prev,
      route_ids: prev.route_ids.includes(routeId)
        ? prev.route_ids.filter(id => id !== routeId)
        : [...prev.route_ids, routeId]
    }));
  };

  if (loading) return <div className="min-h-screen flex items-center justify-center">加载中...</div>;

  return (
    <div className="min-h-screen bg-gray-100 dark:bg-gray-900 py-8">
      <div className="max-w-7xl mx-auto px-4">
        <div className="flex items-center justify-between mb-8">
          <div>
            <Link href="/admin" className="text-blue-500 hover:underline mb-2 inline-block">← 返回后台</Link>
            <h1 className="text-3xl font-bold text-gray-900 dark:text-white">故事管理</h1>
          </div>
          <button onClick={() => { setEditingStory(null); setFormData({ title: "", content: "", cover_url: "", location: "", photo_ids: [], route_ids: [] }); setShowForm(true); }} className="btn-primary">+ 添加故事</button>
        </div>

        {showForm && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div className="card p-6 w-full max-w-3xl max-h-[90vh] overflow-y-auto pb-16">
              <h2 className="text-xl font-bold mb-4">{editingStory ? "编辑故事" : "添加故事"}</h2>
              <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium mb-1">标题 *</label>
                  <input type="text" value={formData.title} onChange={(e) => setFormData({ ...formData, title: e.target.value })} className="input-field" required />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">封面图</label>
                  {formData.photo_ids.length > 0 ? (
                    <div className="space-y-2">
                      <p className="text-xs text-gray-500">从关联照片中选择作为封面：</p>
                      <div className="grid grid-cols-6 gap-2 max-h-32 overflow-y-auto p-2 border rounded-lg">
                        <div 
                          onClick={() => setFormData({ ...formData, cover_url: "" })}
                          className={`relative cursor-pointer rounded-lg overflow-hidden border-2 transition-all ${
                            !formData.cover_url || formData.cover_url === "" ? 'border-blue-500 ring-2 ring-blue-300' : 'border-gray-200 dark:border-gray-700'
                          }`}
                        >
                          <div className="w-full h-16 bg-gray-200 dark:bg-gray-700 flex items-center justify-center text-xs">无</div>
                          {!formData.cover_url && (
                            <div className="absolute top-1 right-1 w-5 h-5 bg-blue-500 rounded-full flex items-center justify-center text-white text-xs">✓</div>
                          )}
                        </div>
                        {photos.filter(p => formData.photo_ids.includes(p.id)).map(photo => (
                          <div 
                            key={photo.id}
                            onClick={() => setFormData({ ...formData, cover_url: photo.url })}
                            className={`relative cursor-pointer rounded-lg overflow-hidden border-2 transition-all ${
                              formData.cover_url === photo.url ? 'border-blue-500 ring-2 ring-blue-300' : 'border-gray-200 dark:border-gray-700'
                            }`}
                          >
                            <div className="relative w-full h-16">
                              <Image src={photo.url} alt={photo.title} fill sizes="4rem" className="object-cover" />
                            </div>
                            {formData.cover_url === photo.url && (
                              <div className="absolute top-1 right-1 w-5 h-5 bg-blue-500 rounded-full flex items-center justify-center text-white text-xs">✓</div>
                            )}
                          </div>
                        ))}
                      </div>
                    </div>
                  ) : (
                    <input type="text" value={formData.cover_url} onChange={(e) => setFormData({ ...formData, cover_url: e.target.value })} className="input-field" placeholder="输入封面图URL" />
                  )}
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">地点</label>
                  <input type="text" value={formData.location} onChange={(e) => setFormData({ ...formData, location: e.target.value })} className="input-field" />
                </div>
                
                {/* 关联照片 */}
                <div>
                  <label className="block text-sm font-medium mb-2">关联照片 ({formData.photo_ids.length}张)</label>
                  {photos.length > 0 ? (
                    <div className="grid grid-cols-4 gap-2 max-h-40 overflow-y-auto p-2 border rounded-lg">
                      {photos.map(photo => (
                        <div 
                          key={photo.id}
                          onClick={() => togglePhoto(photo.id)}
                          className={`relative cursor-pointer rounded-lg overflow-hidden border-2 transition-all ${
                            formData.photo_ids.includes(photo.id) ? 'border-blue-500 ring-2 ring-blue-300' : 'border-gray-200 dark:border-gray-700'
                          }`}
                        >
                          <div className="relative w-full h-16">
                            <Image src={photo.url} alt={photo.title} fill sizes="8rem" className="object-cover" />
                          </div>
                          {formData.photo_ids.includes(photo.id) && (
                            <div className="absolute top-1 right-1 w-5 h-5 bg-blue-500 rounded-full flex items-center justify-center text-white text-xs">✓</div>
                          )}
                          <div className="text-xs truncate px-1 bg-white dark:bg-gray-800">{photo.title}</div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="text-gray-500 text-sm">暂无照片，请先上传照片</p>
                  )}
                </div>
                
                {/* 关联路线 */}
                <div>
                  <label className="block text-sm font-medium mb-2">关联路线 ({formData.route_ids.length}条)</label>
                  {routes.length > 0 ? (
                    <div className="space-y-2 max-h-40 overflow-y-auto p-2 border rounded-lg">
                      {routes.map(route => (
                        <div 
                          key={route.id}
                          onClick={() => toggleRoute(route.id)}
                          className={`flex items-center gap-3 p-2 rounded-lg cursor-pointer border-2 transition-all ${
                            formData.route_ids.includes(route.id) ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/20' : 'border-gray-200 dark:border-gray-700'
                          }`}
                        >
                          <div className={`w-5 h-5 rounded border-2 flex items-center justify-center ${
                            formData.route_ids.includes(route.id) ? 'bg-blue-500 border-blue-500' : 'border-gray-300'
                          }`}>
                            {formData.route_ids.includes(route.id) && <span className="text-white text-xs">✓</span>}
                          </div>
                          <span className="font-medium">{route.title}</span>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="text-gray-500 text-sm">暂无路线，请先创建路线</p>
                  )}
                </div>
                
                <div>
                  <label className="block text-sm font-medium mb-1">内容 *</label>
                  <textarea value={formData.content} onChange={(e) => setFormData({ ...formData, content: e.target.value })} className="input-field" rows={10} required />
                </div>
                <div className="flex gap-4">
                  <button type="submit" className="btn-primary flex-1">保存</button>
                  <button type="button" onClick={() => setShowForm(false)} className="px-6 py-2 border rounded-full">取消</button>
                </div>
              </form>
            </div>
          </div>
        )}

        <div className="space-y-4">
          {stories.map((story) => {
            let photoIds: number[] = [];
            let routeIds: number[] = [];
            try {
              if (story.photo_ids) photoIds = JSON.parse(story.photo_ids);
              if (story.route_ids) routeIds = JSON.parse(story.route_ids);
            } catch {}
            
            return (
              <div key={story.id} className="card p-4 flex items-center justify-between">
                <div className="flex items-center gap-4">
                  {story.cover_url ? <div className="w-16 h-16 relative rounded-lg overflow-hidden"><Image src={story.cover_url} alt={story.title} fill sizes="4rem" className="object-cover" /></div> : <div className="w-16 h-16 bg-gray-200 dark:bg-gray-700 rounded-lg flex items-center justify-center text-2xl">📝</div>}
                  <div>
                    <h3 className="font-semibold">{story.title}</h3>
                    <p className="text-sm text-gray-500">
                      {story.location || "无地点"} · {new Date(story.created_at).toLocaleDateString()}
                      {photoIds.length > 0 && <span className="ml-2 text-blue-500">📷{photoIds.length}</span>}
                      {routeIds.length > 0 && <span className="ml-1 text-green-500">🗺️{routeIds.length}</span>}
                    </p>
                  </div>
                </div>
                <div className="flex gap-2">
                  <button onClick={() => handleEdit(story)} className="px-4 py-2 bg-blue-100 text-blue-600 rounded-full text-sm hover:bg-blue-200">编辑</button>
                  <button onClick={() => handleDelete(story.id)} className="px-4 py-2 bg-red-100 text-red-600 rounded-full text-sm hover:bg-red-200">删除</button>
                </div>
              </div>
            );
          })}
        </div>

        {stories.length === 0 && (
          <div className="text-center py-16 text-gray-500">
            <p className="text-6xl mb-4">📝</p>
            <p>暂无故事，点击上方按钮添加</p>
          </div>
        )}
      </div>
    </div>
  );
}
