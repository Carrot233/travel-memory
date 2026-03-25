"use client";

import { useEffect, useState, useRef } from "react";
import Link from "next/link";
import Image from "next/image";

interface Photo {
  id: number;
  title: string;
  description: string | null;
  url: string;
  location: string | null;
  created_at: string;
}

export default function AdminPhotosPage() {
  const [photos, setPhotos] = useState<Photo[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingPhoto, setEditingPhoto] = useState<Photo | null>(null);
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [formData, setFormData] = useState({
    title: "",
    description: "",
    url: "",
    location: "",
  });

  const fetchPhotos = async () => {
    try {
      const res = await fetch("/api/photos");
      const data = await res.json();
      setPhotos(data);
    } catch (error) {
      console.error("获取照片失败:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPhotos();
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
        setFormData({ ...formData, url: data.url });
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

    if (!formData.url) {
      alert("请上传图片或输入图片URL");
      return;
    }

    const url = editingPhoto ? `/api/photos/${editingPhoto.id}` : "/api/photos";
    const method = editingPhoto ? "PUT" : "POST";

    try {
      const res = await fetch(url, {
        method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(formData),
      });

      if (res.ok) {
        setShowForm(false);
        setEditingPhoto(null);
        setFormData({ title: "", description: "", url: "", location: "" });
        fetchPhotos();
      }
    } catch (error) {
      console.error("保存失败:", error);
    }
  };

  const handleEdit = (photo: Photo) => {
    setEditingPhoto(photo);
    setFormData({
      title: photo.title,
      description: photo.description || "",
      url: photo.url,
      location: photo.location || "",
    });
    setShowForm(true);
  };

  const handleDelete = async (id: number) => {
    if (!confirm("确定要删除这张照片吗？")) return;

    try {
      await fetch(`/api/photos/${id}`, { method: "DELETE" });
      fetchPhotos();
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
              照片管理
            </h1>
          </div>
          <button
            onClick={() => {
              setEditingPhoto(null);
              setFormData({ title: "", description: "", url: "", location: "" });
              setShowForm(true);
            }}
            className="btn-primary"
          >
            + 添加照片
          </button>
        </div>

        {/* Form Modal */}
        {showForm && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div className="card p-6 w-full max-w-md max-h-[90vh] overflow-y-auto">
              <h2 className="text-xl font-bold mb-4">
                {editingPhoto ? "编辑照片" : "添加照片"}
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
                
                {/* 图片上传区域 */}
                <div>
                  <label className="block text-sm font-medium mb-2">图片 *</label>
                  
                  {/* 上传按钮 */}
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
                    {uploading ? "⏳ 上传中..." : "📁 点击上传本地图片"}
                  </button>
                  
                  {/* 预览已上传的图片 */}
                  {formData.url && (
                    <div className="mt-3 relative h-40">
                      <Image
                        src={formData.url}
                        alt="预览"
                        fill
                        sizes="100vw"
                        className="object-cover rounded-lg border"
                      />
                      <button
                        type="button"
                        onClick={() => setFormData({ ...formData, url: "" })}
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
                    value={formData.url.startsWith("/api/uploads/") || formData.url.startsWith("/uploads/") ? "" : formData.url}
                    onChange={(e) => setFormData({ ...formData, url: e.target.value })}
                    className="input-field"
                    placeholder="输入图片 URL（如 https://example.com/image.jpg）"
                    disabled={formData.url.startsWith("/api/uploads/") || formData.url.startsWith("/uploads/")}
                  />
                  {(formData.url.startsWith("/api/uploads/") || formData.url.startsWith("/uploads/")) && (
                    <p className="text-xs text-gray-400 mt-1">已上传本地图片，清空后可输入URL</p>
                  )}
                </div>
                
                <div>
                  <label className="block text-sm font-medium mb-1">地点</label>
                  <input
                    type="text"
                    value={formData.location}
                    onChange={(e) => setFormData({ ...formData, location: e.target.value })}
                    className="input-field"
                    placeholder="北京·故宫"
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
                  <button type="submit" className="btn-primary flex-1" disabled={!formData.url}>
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

        {/* Photos Grid */}
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
          {photos.map((photo) => (
            <div key={photo.id} className="card group">
              <div className="aspect-square relative overflow-hidden">
                <Image
                  src={photo.url}
                  alt={photo.title}
                  fill
                  sizes="(max-width: 768px) 50vw, (max-width: 1024px) 33vw, 25vw"
                  className="object-cover"
                />
                <div className="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-2">
                  <button
                    onClick={() => handleEdit(photo)}
                    className="px-4 py-2 bg-white text-gray-900 rounded-full text-sm hover:bg-gray-100"
                  >
                    编辑
                  </button>
                  <button
                    onClick={() => handleDelete(photo.id)}
                    className="px-4 py-2 bg-red-500 text-white rounded-full text-sm hover:bg-red-600"
                  >
                    删除
                  </button>
                </div>
              </div>
              <div className="p-3">
                <h3 className="font-semibold truncate">{photo.title}</h3>
                {photo.location && (
                  <p className="text-sm text-gray-500">{photo.location}</p>
                )}
              </div>
            </div>
          ))}
        </div>

        {photos.length === 0 && (
          <div className="text-center py-16 text-gray-500">
            <p className="text-6xl mb-4">📷</p>
            <p>暂无照片，点击上方按钮添加</p>
          </div>
        )}
      </div>
    </div>
  );
}
