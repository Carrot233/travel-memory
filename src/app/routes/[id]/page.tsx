"use client";

import { use, useState, useEffect } from "react";
import Link from "next/link";

interface RouteSpot {
  id: number;
  name: string;
  description: string | null;
  latitude: number | null;
  longitude: number | null;
  day_number: number;
  order_index: number;
}

interface RouteInfo {
  id: number;
  title: string;
  description: string | null;
  cover_url: string | null;
  days: number;
  spots: RouteSpot[];
}

export default function RouteDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [route, setRoute] = useState<RouteInfo | null>(null);
  const [activeDay, setActiveDay] = useState(0);

  useEffect(() => {
    fetch(`/api/routes/${id}`)
      .then((res) => res.json())
      .then(setRoute);
  }, [id]);

  if (!route) {
    return <div className="min-h-screen flex items-center justify-center">加载中...</div>;
  }

  // 按天分组景点
  const spotsByDay: { [key: number]: RouteSpot[] } = {};
  route.spots?.forEach((spot) => {
    if (!spotsByDay[spot.day_number]) spotsByDay[spot.day_number] = [];
    spotsByDay[spot.day_number].push(spot);
  });

  // 获取有坐标的景点用于地图显示
  const spotsWithCoords = route.spots?.filter((s) => s.latitude && s.longitude) || [];

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8">
      <div className="max-w-7xl mx-auto px-4">
        <div className="flex items-center justify-between mb-6">
          <Link href="/routes" className="text-blue-500 hover:underline">← 返回路线列表</Link>
          {spotsWithCoords.length > 0 && (
            <Link href={`/routes/${id}/map`} className="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 flex items-center gap-2">
              🗺️ 查看路线图
            </Link>
          )}
        </div>

        {/* 头部 */}
        <div className="card overflow-hidden mb-8">
          {route.cover_url && (
            <img src={route.cover_url} alt={route.title} className="w-full h-64 object-cover" />
          )}
          <div className="p-6">
            <h1 className="text-3xl font-bold mb-2">{route.title}</h1>
            <p className="text-gray-500">📅 {route.days} 天行程</p>
            {route.description && (
              <p className="text-gray-600 mt-4">{route.description}</p>
            )}
          </div>
        </div>

        {/* 地图区域 */}
        {spotsWithCoords.length > 0 && (
          <div className="card p-6 mb-8">
            <h2 className="text-xl font-bold mb-4">📍 路线地图</h2>
            <div className="bg-blue-50 dark:bg-blue-900/30 rounded-xl p-4">
              {/* 按选中的天数显示景点 */}
              <div className="mb-4 flex items-center gap-2">
                <span className="text-sm text-gray-600 dark:text-gray-400">当前显示：</span>
                <span className={`px-3 py-1 rounded-full text-sm ${activeDay === 0 ? 'bg-purple-500 text-white' : 'bg-blue-500 text-white'}`}>
                  {activeDay === 0 ? '全部' : `第 ${activeDay} 天`}
                </span>
              </div>
              <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-4">
                {(activeDay === 0 ? spotsWithCoords : spotsWithCoords.filter(spot => spot.day_number === activeDay))
                  .map((spot) => (
                    <div key={spot.id} className="group relative bg-white dark:bg-gray-800 p-3 rounded-lg">
                      <div className="font-semibold">{spot.name}</div>
                      <div className="text-sm text-gray-500">
                        第 {spot.day_number} 天 · 纬度: {spot.latitude?.toFixed(4)} / 经度: {spot.longitude?.toFixed(4)}
                      </div>
                      {/* 悬停显示描述 */}
                      {spot.description && (
                        <div className="absolute z-10 hidden group-hover:block left-0 right-0 -top-2 -translate-y-full p-3 bg-white/50 dark:bg-gray-900/50 backdrop-blur-sm text-gray-800 dark:text-white text-sm rounded-lg shadow-xl">
                          <p className="line-clamp-3">{spot.description}</p>
                          <div className="absolute bottom-0 left-4 translate-y-1 w-2 h-2 bg-white/50 dark:bg-gray-900/50 rotate-45"></div>
                        </div>
                      )}
                    </div>
                  ))}
              </div>
              {activeDay !== 0 && spotsWithCoords.filter(spot => spot.day_number === activeDay).length === 0 && (
                <p className="text-center text-gray-500 py-4">第 {activeDay} 天暂无坐标数据</p>
              )}
            </div>
          </div>
        )}

        {/* 天数选择 */}
        <div className="flex gap-2 mb-6 overflow-x-auto pb-2">
          <button
            onClick={() => setActiveDay(0)}
            className={`px-6 py-2 rounded-full whitespace-nowrap transition-colors ${
              activeDay === 0
                ? "bg-purple-500 text-white"
                : "bg-gray-200 dark:bg-gray-700 hover:bg-gray-300 dark:hover:bg-gray-600"
            }`}
          >
            全部
          </button>
          {Array.from({ length: route.days }, (_, i) => i + 1).map((day) => (
            <button
              key={day}
              onClick={() => setActiveDay(day)}
              className={`px-6 py-2 rounded-full whitespace-nowrap transition-colors ${
                activeDay === day
                  ? "bg-blue-500 text-white"
                  : "bg-gray-200 dark:bg-gray-700 hover:bg-gray-300 dark:hover:bg-gray-600"
              }`}
            >
              第 {day} 天
            </button>
          ))}
        </div>

        {/* 景点列表 */}
        <div className="space-y-4">
          {(() => {
            const activeSpots = activeDay === 0 
              ? Object.values(spotsByDay).flat() 
              : spotsByDay[activeDay] || [];
            return activeSpots.length > 0 ? (
              activeSpots.map((spot, index) => (
                <div key={spot.id} className="card p-6 flex gap-4">
                  <div className="w-12 h-12 bg-blue-500 text-white rounded-full flex items-center justify-center text-xl font-bold shrink-0">
                    {index + 1}
                  </div>
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <h3 className="text-xl font-semibold">{spot.name}</h3>
                      {activeDay === 0 && (
                        <span className="text-xs px-2 py-1 bg-blue-100 text-blue-600 rounded-full">第{spot.day_number}天</span>
                      )}
                    </div>
                    {spot.description && (
                      <p className="text-gray-600 mt-2">{spot.description}</p>
                    )}
                    {spot.latitude && spot.longitude && (
                      <p className="text-sm text-green-500 mt-2">📍 已标记位置</p>
                    )}
                  </div>
                </div>
              ))
            ) : (
              <div className="text-center py-12 text-gray-500">
                {activeDay === 0 ? '暂无景点安排' : `第 ${activeDay} 天暂无景点安排`}
              </div>
            );
          })()}
        </div>
      </div>
    </div>
  );
}
