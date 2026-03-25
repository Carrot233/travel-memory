"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";

interface Stats {
  photos: number;
  routes: number;
  videos: number;
  stories: number;
}

export default function AdminPage() {
  const router = useRouter();
  const [stats, setStats] = useState<Stats | null>(null);
  const [loading, setLoading] = useState(true);
  const [isAdmin, setIsAdmin] = useState(false);

  useEffect(() => {
    // 检查登录状态
    fetch("/api/auth/session")
      .then((res) => res.json())
      .then((data) => {
        if (!data?.user) {
          router.push("/login");
          return;
        }
        setIsAdmin(data.user.role === "admin");
        // 获取统计数据
        return fetch("/api/admin/stats");
      })
      .then((res) => res?.json())
      .then((data) => {
        if (data) setStats(data);
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [router]);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-xl">加载中...</div>
      </div>
    );
  }

  const menuItems = [
    { href: "/admin/photos", icon: "📷", label: "照片管理", color: "bg-blue-500" },
    { href: "/admin/routes", icon: "🗺️", label: "路线管理", color: "bg-teal-500" },
    { href: "/admin/videos", icon: "🎬", label: "视频管理", color: "bg-green-500" },
    { href: "/admin/stories", icon: "📝", label: "故事管理", color: "bg-purple-500" },
  ];

  return (
    <div className="min-h-screen bg-gray-100 dark:bg-gray-900 py-8">
      <div className="max-w-7xl mx-auto px-4">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 dark:text-white">
            管理后台
          </h1>
          <p className="text-gray-500 mt-2">管理您的旅游内容</p>
        </div>

        {/* Stats Cards */}
        {stats && (
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
            <div className="card p-6">
              <div className="text-3xl font-bold text-blue-500">{stats.photos}</div>
              <div className="text-gray-500 mt-1">照片</div>
            </div>
            <div className="card p-6">
              <div className="text-3xl font-bold text-teal-500">{stats.routes}</div>
              <div className="text-gray-500 mt-1">路线</div>
            </div>
            <div className="card p-6">
              <div className="text-3xl font-bold text-green-500">{stats.videos}</div>
              <div className="text-gray-500 mt-1">视频</div>
            </div>
            <div className="card p-6">
              <div className="text-3xl font-bold text-purple-500">{stats.stories}</div>
              <div className="text-gray-500 mt-1">故事</div>
            </div>
          </div>
        )}

        {/* Menu Grid */}
        <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
          {menuItems.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className="card p-6 hover:shadow-xl transition-shadow group"
            >
              <div className={`w-16 h-16 ${item.color} rounded-2xl flex items-center justify-center text-3xl group-hover:scale-110 transition-transform`}>
                {item.icon}
              </div>
              <h3 className="text-xl font-semibold text-gray-900 dark:text-white mt-4">
                {item.label}
              </h3>
              <p className="text-gray-500 mt-2">添加、编辑和删除{item.label.replace("管理", "")}</p>
            </Link>
          ))}
        </div>

        {/* Quick Actions */}
        <div className="mt-8 card p-6">
          <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">
            快捷操作
          </h2>
          <div className="flex flex-wrap gap-4">
            <Link href="/" className="btn-primary">
              查看网站
            </Link>
            <button
              onClick={() => {
                fetch("/api/auth/logout", { method: "POST" });
                router.push("/");
              }}
              className="px-6 py-2 border border-gray-300 rounded-full text-gray-600 hover:bg-gray-100"
            >
              退出登录
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
