"use client";

import { useEffect, useState, use, useRef } from "react";
import Link from "next/link";
import dynamic from "next/dynamic";

// 动态导入地图组件，避免 SSR 问题
const LocationPicker = dynamic(() => import("@/components/LocationPicker"), {
  ssr: false,
  loading: () => <div className="w-full h-64 bg-gray-100 rounded-lg flex items-center justify-center">地图加载中...</div>,
});

interface RouteInfo {
  id: number;
  title: string;
  days: number;
}

interface RouteSpot {
  id: number;
  route_id: number;
  name: string;
  description: string | null;
  image_url: string | null;
  latitude: number | null;
  longitude: number | null;
  day_number: number;
  order_index: number;
}

export default function EditRouteSpotsPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [route, setRoute] = useState<RouteInfo | null>(null);
  const [spots, setSpots] = useState<RouteSpot[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingSpot, setEditingSpot] = useState<RouteSpot | null>(null);
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [formData, setFormData] = useState({
    name: "",
    description: "",
    image_url: "",
    latitude: "",
    longitude: "",
    day_number: 1,
    order_index: 0,
  });

  const fetchData = async () => {
    try {
      const res = await fetch(`/api/routes/${id}`);
      const data = await res.json();
      setRoute(data);
      setSpots(data.spots || []);
    } catch (error) {
      console.error("获取数据失败:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [id]);

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
        setFormData({ ...formData, image_url: data.url });
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

  // 处理地图选择位置
  const handleLocationSelect = (location: {
    name: string;
    latitude: number;
    longitude: number;
    address?: string;
  }) => {
    // 使用选中位置的名称（建筑名称），覆盖原有名称
    setFormData(prev => ({
      ...prev,
      name: location.name, // 自动填充为地点建筑名称
      latitude: location.latitude.toString(),
      longitude: location.longitude.toString(),
    }));
    
    // 自动获取地标图片（如果没有手动上传）
    if (!formData.image_url) {
      autoFetchImage(location.name);
    }
  };
  
  // 自动获取地标图片（使用 Wikimedia Commons API）
  const autoFetchImage = async (locationName: string) => {
    try {
      // 搜索维基百科图片
      const searchUrl = `https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=${encodeURIComponent(locationName)}&format=json&origin=*`;
      const searchRes = await fetch(searchUrl);
      const searchData = await searchRes.json();
      
      if (searchData.query?.search?.[0]) {
        const pageId = searchData.query.search[0].pageid;
        
        // 获取该页面的图片
        const imgUrl = `https://en.wikipedia.org/w/api.php?action=query&pageids=${pageId}&prop=pageimages&pithumbsize=400&format=json&origin=*`;
        const imgRes = await fetch(imgUrl);
        const imgData = await imgRes.json();
        
        const pages = imgData.query?.pages || {};
        const page = pages[pageId];
        
        if (page?.thumbnail?.source) {
          // 下载图片并上传到服务器
          await downloadAndUploadImage(page.thumbnail.source);
        }
      }
    } catch (error) {
      console.error("自动获取图片失败:", error);
    }
  };
  
  // 下载图片并上传到服务器
  const downloadAndUploadImage = async (imageUrl: string) => {
    try {
      // 先下载图片
      const imgRes = await fetch(imageUrl);
      const blob = await imgRes.blob();
      
      // 转换为 File 对象
      const ext = imageUrl.split('.').pop() || 'jpg';
      const fileName = `auto_${Date.now()}.${ext}`;
      const file = new File([blob], fileName, { type: blob.type });
      
      // 上传到服务器
      const uploadFormData = new FormData();
      uploadFormData.append("file", file);
      uploadFormData.append("type", "photos");
      
      const res = await fetch("/api/upload", {
        method: "POST",
        body: uploadFormData,
      });
      
      const data = await res.json();
      if (res.ok && data.url) {
        setFormData(prev => ({ ...prev, image_url: data.url }));
      }
    } catch (error) {
      console.error("上传自动图片失败:", error);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!formData.name) {
      alert("请输入景点名称");
      return;
    }

    const spotData = {
      ...formData,
      latitude: formData.latitude ? parseFloat(formData.latitude) : null,
      longitude: formData.longitude ? parseFloat(formData.longitude) : null,
    };

    const url = editingSpot ? `/api/spots/${editingSpot.id}` : `/api/routes/${id}/spots`;
    const method = editingSpot ? "PUT" : "POST";

    try {
      const res = await fetch(url, {
        method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(spotData),
      });

      if (res.ok) {
        setShowForm(false);
        setEditingSpot(null);
        setFormData({ name: "", description: "", image_url: "", latitude: "", longitude: "", day_number: 1, order_index: 0 });
        fetchData();
      }
    } catch (error) {
      console.error("保存失败:", error);
    }
  };

  const handleEdit = (spot: RouteSpot) => {
    setEditingSpot(spot);
    setFormData({
      name: spot.name,
      description: spot.description || "",
      image_url: spot.image_url || "",
      latitude: spot.latitude?.toString() || "",
      longitude: spot.longitude?.toString() || "",
      day_number: spot.day_number,
      order_index: spot.order_index,
    });
    setShowForm(true);
  };

  const handleDelete = async (spotId: number) => {
    if (!confirm("确定要删除这个景点吗？")) return;

    try {
      await fetch(`/api/spots/${spotId}`, { method: "DELETE" });
      fetchData();
    } catch (error) {
      console.error("删除失败:", error);
    }
  };

  if (loading) {
    return <div className="min-h-screen flex items-center justify-center">加载中...</div>;
  }

  if (!route) {
    return <div className="min-h-screen flex items-center justify-center">路线不存在</div>;
  }

  // 按天分组
  const spotsByDay: { [key: number]: RouteSpot[] } = {};
  spots.forEach((spot) => {
    if (!spotsByDay[spot.day_number]) spotsByDay[spot.day_number] = [];
    spotsByDay[spot.day_number].push(spot);
  });

  return (
    <div className="min-h-screen bg-gray-100 dark:bg-gray-900 py-8">
      <div className="max-w-7xl mx-auto px-4">
        <div className="flex items-center justify-between mb-8">
          <div>
            <Link href="/admin/routes" className="text-blue-500 hover:underline mb-2 inline-block">
              ← 返回路线管理
            </Link>
            <h1 className="text-3xl font-bold text-gray-900 dark:text-white">
              编辑景点：{route.title}
            </h1>
            <p className="text-gray-500 mt-1">共 {route.days} 天行程</p>
          </div>
          <button
            onClick={() => {
              setEditingSpot(null);
              setFormData({ name: "", description: "", image_url: "", latitude: "", longitude: "", day_number: 1, order_index: spots.length });
              setShowForm(true);
            }}
            className="btn-primary"
          >
            + 添加景点
          </button>
        </div>

        {/* Form Modal */}
        {showForm && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4 overflow-y-auto">
            <div className="card p-6 w-full max-w-lg my-8">
              <h2 className="text-xl font-bold mb-4">
                {editingSpot ? "编辑景点" : "添加景点"}
              </h2>
              <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium mb-1">景点名称 *</label>
                  <input
                    type="text"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    className="input-field"
                    placeholder="从地图选择后自动填充，或手动输入"
                    required
                  />
                </div>
                
                {/* 地图选择位置 */}
                <div>
                  <label className="block text-sm font-medium mb-2">选择位置（地图）</label>
                  <LocationPicker
                    onLocationSelect={handleLocationSelect}
                    initialLatitude={formData.latitude ? parseFloat(formData.latitude) : null}
                    initialLongitude={formData.longitude ? parseFloat(formData.longitude) : null}
                    initialName={formData.name}
                  />
                </div>

                {/* 经纬度显示/编辑 */}
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium mb-1">纬度</label>
                    <input
                      type="text"
                      value={formData.latitude}
                      onChange={(e) => setFormData({ ...formData, latitude: e.target.value })}
                      className="input-field"
                      placeholder="点击地图自动获取"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium mb-1">经度</label>
                    <input
                      type="text"
                      value={formData.longitude}
                      onChange={(e) => setFormData({ ...formData, longitude: e.target.value })}
                      className="input-field"
                      placeholder="点击地图自动获取"
                    />
                  </div>
                </div>
                
                {/* 景点图片上传 */}
                <div>
                  <label className="block text-sm font-medium mb-2">景点图片</label>
                  
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
                    {uploading ? "⏳ 上传中..." : "📁 点击上传景点图片"}
                  </button>
                  
                  {formData.image_url && (
                    <div className="mt-3 relative">
                      <img
                        src={formData.image_url}
                        alt="预览"
                        className="w-full h-32 object-cover rounded-lg border"
                      />
                      <button
                        type="button"
                        onClick={() => setFormData({ ...formData, image_url: "" })}
                        className="absolute top-2 right-2 bg-red-500 text-white rounded-full w-6 h-6 flex items-center justify-center hover:bg-red-600"
                      >
                        ×
                      </button>
                    </div>
                  )}
                  
                  <div className="flex items-center my-3">
                    <div className="flex-1 border-t border-gray-300"></div>
                    <span className="px-3 text-gray-400 text-sm">或</span>
                    <div className="flex-1 border-t border-gray-300"></div>
                  </div>
                  
                  <input
                    type="url"
                    value={formData.image_url.startsWith("/api/uploads/") || formData.image_url.startsWith("/uploads/") ? "" : formData.image_url}
                    onChange={(e) => setFormData({ ...formData, image_url: e.target.value })}
                    className="input-field"
                    placeholder="输入图片 URL"
                    disabled={formData.image_url.startsWith("/api/uploads/") || formData.image_url.startsWith("/uploads/")}
                  />
                </div>
                
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium mb-1">第几天</label>
                    <input
                      type="number"
                      min="1"
                      max={route.days}
                      value={formData.day_number}
                      onChange={(e) => setFormData({ ...formData, day_number: parseInt(e.target.value) || 1 })}
                      className="input-field"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium mb-1">排序</label>
                    <input
                      type="number"
                      min="0"
                      value={formData.order_index}
                      onChange={(e) => setFormData({ ...formData, order_index: parseInt(e.target.value) || 0 })}
                      className="input-field"
                    />
                  </div>
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

        {/* Spots by Day */}
        {Object.keys(spotsByDay).length > 0 ? (
          <div className="space-y-8">
            {Array.from({ length: route.days }, (_, i) => i + 1).map((day) => (
              <div key={day}>
                <h2 className="text-xl font-bold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
                  <span className="w-8 h-8 bg-blue-500 text-white rounded-full flex items-center justify-center text-sm">
                    {day}
                  </span>
                  第 {day} 天
                </h2>
                <div className="grid gap-4">
                  {(spotsByDay[day] || []).map((spot) => (
                    <div key={spot.id} className="card p-4 flex items-center gap-4">
                      {/* 景点图片 */}
                      <div className="w-20 h-20 flex-shrink-0 rounded-lg overflow-hidden bg-gray-200 dark:bg-gray-700">
                        {spot.image_url ? (
                          <img src={spot.image_url} alt={spot.name} className="w-full h-full object-cover" />
                        ) : (
                          <div className="w-full h-full flex items-center justify-center text-2xl">📍</div>
                        )}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2">
                          <span className="w-6 h-6 bg-gray-100 dark:bg-gray-700 rounded-full flex items-center justify-center text-xs">
                            {spot.order_index + 1}
                          </span>
                          <h3 className="font-semibold">{spot.name}</h3>
                        </div>
                        <div className="text-sm text-gray-500 mt-1">
                          {spot.latitude && spot.longitude && (
                            <span className="text-green-500 mr-2">📍 已设置位置</span>
                          )}
                          {spot.image_url && (
                            <span className="text-blue-500 mr-2">🖼️ 有图片</span>
                          )}
                        </div>
                        {spot.description && (
                          <p className="text-sm text-gray-400 truncate mt-1">{spot.description}</p>
                        )}
                      </div>
                      <div className="flex gap-2 flex-shrink-0">
                        <button
                          onClick={() => handleEdit(spot)}
                          className="px-3 py-1 text-sm bg-blue-100 text-blue-600 rounded hover:bg-blue-200"
                        >
                          编辑
                        </button>
                        <button
                          onClick={() => handleDelete(spot.id)}
                          className="px-3 py-1 text-sm bg-red-100 text-red-600 rounded hover:bg-red-200"
                        >
                          删除
                        </button>
                      </div>
                    </div>
                  ))}
                  {(!spotsByDay[day] || spotsByDay[day].length === 0) && (
                    <div className="text-gray-400 text-center py-4">
                      暂无景点，点击右上角添加
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-16 text-gray-500">
            <p className="text-6xl mb-4">📍</p>
            <p>暂无景点，点击右上角按钮添加</p>
          </div>
        )}
      </div>
    </div>
  );
}
