"use client";

import { useEffect, useState, useRef } from "react";
import Link from "next/link";
import Image from "next/image";

interface Route {
  id: number;
  title: string;
  description: string | null;
  cover_url: string | null;
  days: number;
  created_at: string;
}

export default function AdminRoutesPage() {
  const [routes, setRoutes] = useState<Route[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingRoute, setEditingRoute] = useState<Route | null>(null);
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [formData, setFormData] = useState({
    title: "",
    description: "",
    cover_url: "",
    days: 1,
  });

  const fetchRoutes = async () => {
    try {
      const res = await fetch("/api/routes");
      const data = await res.json();
      setRoutes(data);
    } catch (error) {
      console.error("获取路线失败:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRoutes();
  }, []);

  // 处理文件上传
  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setUploading(true);
    try {
      const uploadFormData = new FormData();
      uploadFormData.append("file", file);
      uploadFormData.append("type", "photos");

      const res = await fetch("/api/upload", {
        method: "POST",
        body: uploadFormData,
      });

      const data = await res.json();
      if (res.ok && data.url) {
        setFormData({ ...formData, cover_url: data.url });
      } else {
        alert(data.error || "上传失败");
      }
    } catch (error) {
      console.error("上传失败:", error);
      alert("上传失败，请重试");
    } finally {
      setUploading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const url = editingRoute ? `/api/routes/${editingRoute.id}` : "/api/routes";
    const method = editingRoute ? "PUT" : "POST";

    try {
      const res = await fetch(url, {
        method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(formData),
      });

      if (res.ok) {
        setShowForm(false);
        setEditingRoute(null);
        setFormData({ title: "", description: "", cover_url: "", days: 1 });
        fetchRoutes();
      }
    } catch (error) {
      console.error("保存失败:", error);
    }
  };

  const handleEdit = (route: Route) => {
    setEditingRoute(route);
    setFormData({
      title: route.title,
      description: route.description || "",
      cover_url: route.cover_url || "",
      days: route.days,
    });
    setShowForm(true);
  };

  const handleDelete = async (id: number) => {
    if (!confirm("确定要删除这条路线吗？相关景点也会被删除。")) return;

    try {
      await fetch(`/api/routes/${id}`, { method: "DELETE" });
      fetchRoutes();
    } catch (error) {
      console.error("删除失败:", error);
    }
  };

  if (loading) {
    return <div className="min-h-screen flex items-center justify-center">加载中...</div>;
  }

  return (
    <div className="min-h-screen bg-gray-100 dark:bg-gray-900 py-8">
      <div className="max-w-7xl mx-auto px-4">
        <div className="flex items-center justify-between mb-8">
          <div>
            <Link href="/admin" className="text-blue-500 hover:underline mb-2 inline-block">
              ← 返回后台
            </Link>
            <h1 className="text-3xl font-bold text-gray-900 dark:text-white">
              路线管理
            </h1>
          </div>
          <button
            onClick={() => {
              setEditingRoute(null);
              setFormData({ title: "", description: "", cover_url: "", days: 1 });
              setShowForm(true);
            }}
            className="btn-primary"
          >
            + 添加路线
          </button>
        </div>

        {/* Form Modal */}
        {showForm && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div className="card p-6 w-full max-w-md max-h-[90vh] overflow-y-auto">
              <h2 className="text-xl font-bold mb-4">
                {editingRoute ? "编辑路线" : "添加路线"}
              </h2>
              <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium mb-1">标题 *</label>
                  <input
                    type="text"
                    value={formData.title}
                    onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                    className="input-field"
                    required
                  />
                </div>
                
                {/* 封面图上传区域 */}
                <div>
                  <label className="block text-sm font-medium mb-2">封面图</label>
                  
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/jpeg,image/png,image/gif,image/webp"
                    onChange={handleFileUpload}
                    className="hidden"
                  />
                  <button
                    type="button"
                    onClick={() => fileInputRef.current?.click()}
                    disabled={uploading}
                    className="w-full py-3 px-4 border-2 border-dashed border-gray-300 rounded-lg hover:border-blue-500 transition-colors disabled:opacity-50 text-center"
                  >
                    {uploading ? "⏳ 上传中..." : "📁 点击上传封面图"}
                  </button>
                  
                  {/* 预览已上传的图片 */}
                  {formData.cover_url && (
                    <div className="mt-3 relative h-40">
                      <Image
                        src={formData.cover_url}
                        alt="预览"
                        fill
                        sizes="100vw"
                        className="object-cover rounded-lg border"
                      />
                      <button
                        type="button"
                        onClick={() => setFormData({ ...formData, cover_url: "" })}
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
                  
                  {/* 输入 URL */}
                  <input
                    type="url"
                    value={formData.cover_url.startsWith("/api/uploads/") || formData.cover_url.startsWith("/uploads/") ? "" : formData.cover_url}
                    onChange={(e) => setFormData({ ...formData, cover_url: e.target.value })}
                    className="input-field"
                    placeholder="输入封面图 URL"
                    disabled={formData.cover_url.startsWith("/api/uploads/") || formData.cover_url.startsWith("/uploads/")}
                  />
                  {(formData.cover_url.startsWith("/api/uploads/") || formData.cover_url.startsWith("/uploads/")) && (
                    <p className="text-xs text-gray-400 mt-1">已上传本地图片，清空后可输入URL</p>
                  )}
                </div>
                
                <div>
                  <label className="block text-sm font-medium mb-1">天数</label>
                  <input
                    type="number"
                    min="1"
                    value={formData.days}
                    onChange={(e) => setFormData({ ...formData, days: parseInt(e.target.value) || 1 })}
                    className="input-field"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">描述</label>
                  <textarea
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    className="input-field"
                    rows={3}
                  />
                </div>
                <div className="flex gap-4">
                  <button type="submit" className="btn-primary flex-1">
                    保存
                  </button>
                  <button
                    type="button"
                    onClick={() => setShowForm(false)}
                    className="px-6 py-2 border rounded-full"
                  >
                    取消
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* Routes List */}
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          {routes.map((route) => (
            <div key={route.id} className="card group">
              <div className="h-40 relative overflow-hidden">
                {route.cover_url ? (
                  <Image
                    src={route.cover_url}
                    alt={route.title}
                    fill
                    sizes="(max-width: 768px) 100vw, (max-width: 1024px) 50vw, 33vw"
                    className="object-cover"
                  />
                ) : (
                  <div className="w-full h-full bg-gradient-to-br from-teal-400 to-blue-500 flex items-center justify-center text-4xl">
                    🗺️
                  </div>
                )}
                <div className="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-2">
                  <Link
                    href={`/admin/routes/${route.id}`}
                    className="px-4 py-2 bg-white text-gray-900 rounded-full text-sm hover:bg-gray-100"
                  >
                    编辑景点
                  </Link>
                  <button
                    onClick={() => handleEdit(route)}
                    className="px-4 py-2 bg-blue-500 text-white rounded-full text-sm hover:bg-blue-600"
                  >
                    编辑
                  </button>
                  <button
                    onClick={() => handleDelete(route.id)}
                    className="px-4 py-2 bg-red-500 text-white rounded-full text-sm hover:bg-red-600"
                  >
                    删除
                  </button>
                </div>
              </div>
              <div className="p-4">
                <h3 className="font-semibold text-lg">{route.title}</h3>
                <p className="text-sm text-gray-500 mt-1">📅 {route.days} 天行程</p>
                {route.description && (
                  <p className="text-sm text-gray-400 mt-2 line-clamp-2">{route.description}</p>
                )}
              </div>
            </div>
          ))}
        </div>

        {routes.length === 0 && (
          <div className="text-center py-16 text-gray-500">
            <p className="text-6xl mb-4">🗺️</p>
            <p>暂无路线，点击上方按钮添加</p>
          </div>
        )}
      </div>
    </div>
  );
}
