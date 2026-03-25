"use client";

import { useEffect, useState, use } from "react";
import Link from "next/link";

interface RouteSpot {
  id: number;
  name: string;
  description: string | null;
  latitude: number;
  longitude: number;
  day_number: number;
  order_index: number;
}

interface RouteData {
  id: number;
  title: string;
  days: number;
  spots: RouteSpot[];
}

interface Photo {
  id: number;
  title: string;
  url: string;
  location: string;
  spot_id: number | null;
}

export default function RouteMapPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [route, setRoute] = useState<RouteData | null>(null);
  const [photos, setPhotos] = useState<Photo[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedDay, setSelectedDay] = useState<number | null>(null);
  const [selectedPhoto, setSelectedPhoto] = useState<Photo | null>(null);

  useEffect(() => {
    // 获取路线数据
    fetch(`/api/routes/${id}`)
      .then(res => {
        if (!res.ok) throw new Error('API error');
        return res.json();
      })
      .then(data => {
        const sortedSpots = (data.spots || []).sort((a: RouteSpot, b: RouteSpot) => {
          if (a.day_number !== b.day_number) return a.day_number - b.day_number;
          return a.order_index - b.order_index;
        });
        data.spots = sortedSpots;
        setRoute(data);
        // 获取该路线的照片
        return fetch(`/api/photos?route_id=${id}`);
      })
      .then(res => res?.json())
      .then(photoData => {
        setPhotos(photoData || []);
        setLoading(false);
      })
      .catch(err => {
        console.error('Fetch error:', err);
        setLoading(false);
      });
  }, [id]);

  const getValidSpots = () => {
    if (!route) return [];
    const spots = selectedDay === null ? route.spots : route.spots.filter(s => s.day_number === selectedDay);
    return spots.filter(s => s.latitude && s.longitude);
  };

  const getBounds = (spots: RouteSpot[]) => {
    const lats = spots.map(s => s.latitude);
    const lngs = spots.map(s => s.longitude);
    return {
      minLat: Math.min(...lats),
      maxLat: Math.max(...lats),
      minLng: Math.min(...lngs),
      maxLng: Math.max(...lngs),
    };
  };

  const calculateDistance = (lat1: number, lon1: number, lat2: number, lon2: number) => {
    const R = 6371;
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
              Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
              Math.sin(dLon/2) * Math.sin(dLon/2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    return R * c;
  };

  const handleDayClick = (day: number | null) => {
    setSelectedDay(day);
  };

  // 获取某个景点的照片（通过 spot_id 或 location 匹配）
  const getSpotPhotos = (spotId: number, spotName: string) => {
    return photos.filter(p => 
      p.spot_id === spotId || 
      (p.location && spotName.includes(p.location)) ||
      (p.location && p.location.includes(spotName))
    );
  };

  if (loading) {
    return (
      <div className="h-screen bg-gray-900 flex items-center justify-center">
        <div className="text-white">加载中...</div>
      </div>
    );
  }

  const spots = getValidSpots();
  const bounds = getBounds(spots);
  const svgWidth = 800;
  const svgHeight = 800;
  const padding = 80;

  const spotsByDay: { [key: number]: RouteSpot[] } = {};
  spots.forEach(spot => {
    if (!spotsByDay[spot.day_number]) spotsByDay[spot.day_number] = [];
    spotsByDay[spot.day_number].push(spot);
  });

  const days = Object.keys(spotsByDay).map(Number).sort((a, b) => a - b);

  const colors = [
    { main: "#60a5fa", light: "#93c5fd" },
    { main: "#34d399", light: "#6ee7b7" },
    { main: "#fbbf24", light: "#fcd34d" },
    { main: "#f87171", light: "#fca5a5" },
    { main: "#a78bfa", light: "#c4b5fd" },
    { main: "#f472b6", light: "#f9a8d4" },
  ];

  const CENTER = { 
    lat: (bounds.maxLat + bounds.minLat) / 2, 
    lng: (bounds.maxLng + bounds.minLng) / 2 
  };

  // 计算节点大小 - 基于与上一个点的距离
  const getNodeSize = (spot: RouteSpot, daySpots: RouteSpot[], idx: number) => {
    if (idx === 0) return 18;
    const prevSpot = daySpots[idx - 1];
    const distance = calculateDistance(prevSpot.latitude, prevSpot.longitude, spot.latitude, spot.longitude);
    return Math.min(28, Math.max(14, 14 + distance / 50));
  };

  // 地理坐标位置
  const getGeoPosition = (spot: RouteSpot) => {
    const dLat = spot.latitude - CENTER.lat;
    const dLng = spot.longitude - CENTER.lng;
    const scale = 20;
    let x = svgWidth / 2 + dLng * scale;
    let y = svgHeight / 2 - dLat * scale;
    x = Math.max(padding, Math.min(svgWidth - padding, x));
    y = Math.max(padding, Math.min(svgHeight - padding, y));
    return { x, y };
  };

  // 调整节点位置 - 仅圆和圆不能交叉
  const adjustPositions = () => {
    const positions: { [key: string]: { x: number; y: number } } = {};
    const minGap = 50;
    
    // 初始化为地理位置
    spots.forEach(spot => {
      const key = `${spot.day_number}-${spot.order_index}`;
      positions[key] = getGeoPosition(spot);
    });

    // 迭代分离冲突的圆
    const maxIterations = 150;
    for (let iter = 0; iter < maxIterations; iter++) {
      let hasConflict = false;
      
      // 检查圆与圆之间的冲突
      for (let i = 0; i < spots.length; i++) {
        for (let j = i + 1; j < spots.length; j++) {
          const spotA = spots[i];
          const spotB = spots[j];
          const keyA = `${spotA.day_number}-${spotA.order_index}`;
          const keyB = `${spotB.day_number}-${spotB.order_index}`;
          const posA = positions[keyA];
          const posB = positions[keyB];
          
          const daySpotsA = spotsByDay[spotA.day_number] || [];
          const daySpotsB = spotsByDay[spotB.day_number] || [];
          const idxA = daySpotsA.findIndex(s => s.order_index === spotA.order_index);
          const idxB = daySpotsB.findIndex(s => s.order_index === spotB.order_index);
          const radiusA = getNodeSize(spotA, daySpotsA, idxA >= 0 ? idxA : 0);
          const radiusB = getNodeSize(spotB, daySpotsB, idxB >= 0 ? idxB : 0);
          
          const dx = posB.x - posA.x;
          const dy = posB.y - posA.y;
          const dist = Math.sqrt(dx * dx + dy * dy);
          const minDist = radiusA + radiusB + minGap;
          
          if (dist < minDist && dist > 0) {
            hasConflict = true;
            const overlap = minDist - dist;
            const nx = dx / dist;
            const ny = dy / dist;
            const moveX = nx * overlap * 0.5;
            const moveY = ny * overlap * 0.5;
            positions[keyB] = { x: posB.x + moveX, y: posB.y + moveY };
          }
        }
      }
      
      if (!hasConflict) break;
    }

    return positions;
  };

  const positions = adjustPositions();

  const getPosition = (spot: RouteSpot) => {
    return positions[`${spot.day_number}-${spot.order_index}`] || getGeoPosition(spot);
  };

  return (
    <div className="min-h-screen bg-gray-900 text-white">
      <div className="bg-gray-800 px-4 py-3 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Link href={`/routes/${id}`} className="text-gray-400 hover:text-white">← 返回</Link>
          <h1 className="text-lg font-bold">{route?.title}</h1>
        </div>
        <div className="text-sm text-gray-400">
          共 {spots.length} 个景点 · {route?.days} 天
        </div>
      </div>

      <div className="bg-gray-800 px-4 py-2 flex gap-2 overflow-x-auto">
        <button 
          onClick={() => handleDayClick(null)} 
          className={`px-3 py-1 rounded-full text-sm ${selectedDay === null ? "bg-white text-gray-900" : "bg-gray-700 text-gray-300"}`}
        >
          全部
        </button>
        {route && Array.from({ length: route.days }, (_, i) => i + 1).map(day => {
          const daySpots = spotsByDay[day] || [];
          return (
            <button 
              key={day} 
              onClick={() => handleDayClick(day)} 
              className={`px-3 py-1 rounded-full text-sm ${selectedDay === day ? "text-gray-900" : "bg-gray-700 text-gray-300"}`}
              style={selectedDay === day ? { backgroundColor: colors[(day - 1) % colors.length].main } : {}}
            >
              第{day}天 ({daySpots.length}站)
            </button>
          );
        })}
      </div>

      <div className="p-4">
        <div className="bg-gray-800 rounded-xl p-6 overflow-auto relative">
          <svg viewBox={`0 0 ${svgWidth} ${svgHeight}`} className="w-full h-auto">
            <defs>
              <radialGradient id="bgGrad" cx="50%" cy="50%" r="70%">
                <stop offset="0%" stopColor="#1f2937" />
                <stop offset="100%" stopColor="#111827" />
              </radialGradient>
              {colors.map((c, i) => (
                <filter key={i} id={`glow-${i}`}>
                  <feGaussianBlur stdDeviation="4" result="coloredBlur"/>
                  <feMerge>
                    <feMergeNode in="coloredBlur"/>
                    <feMergeNode in="SourceGraphic"/>
                  </feMerge>
                </filter>
              ))}
              <marker id="arrow" markerWidth="4" markerHeight="4" refX="3" refY="2" orient="auto" markerUnits="strokeWidth">
                <path d="M0,0 L4,2 L0,4 L1,2 Z" fill="none" stroke="context-stroke" strokeWidth="0.5" />
              </marker>
            </defs>
            
            <rect width="100%" height="100%" fill="url(#bgGrad)" rx="12" />
            <text x="30" y="25" fill="#4b5563" fontSize="5">西北</text>
            <text x={svgWidth - 50} y="25" fill="#4b5563" fontSize="5">东北</text>
            <text x="30" y={svgHeight - 15} fill="#4b5563" fontSize="5">西南</text>
            <text x={svgWidth - 50} y={svgHeight - 15} fill="#4b5563" fontSize="5">东南</text>

            {/* 绘制连线 - 每段都有箭头，箭头指向圆的边缘 */}
            {days.map((day, dayIdx) => {
              const daySpots = (spotsByDay[day] || []).sort((a, b) => a.order_index - b.order_index);
              if (daySpots.length < 1) return null;
              const color = colors[dayIdx % colors.length];
              const isDimmed = selectedDay !== null && selectedDay !== day;

              return (
                <g key={`paths-${day}`} opacity={isDimmed ? 0.15 : 1}>
                  {/* 连接前一天的最后一个点 */}
                  {dayIdx > 0 && (() => {
                    const prevDaySpots = (spotsByDay[days[dayIdx - 1]] || []).sort((a, b) => a.order_index - b.order_index);
                    if (prevDaySpots.length > 0) {
                      const prevSpot = prevDaySpots[prevDaySpots.length - 1];
                      const currSpot = daySpots[0];
                      const prevPos = getPosition(prevSpot);
                      const currPos = getPosition(currSpot);
                      const prevSize = getNodeSize(prevSpot, prevDaySpots, prevDaySpots.length - 1);
                      const currSize = getNodeSize(currSpot, daySpots, 0);
                      
                      // 计算方向向量
                      const dx = currPos.x - prevPos.x;
                      const dy = currPos.y - prevPos.y;
                      const dist = Math.sqrt(dx * dx + dy * dy) || 1;
                      const nx = dx / dist;
                      const ny = dy / dist;
                      
                      // 起点和终点都在圆边缘
                      const startX = prevPos.x + nx * prevSize;
                      const startY = prevPos.y + ny * prevSize;
                      const endX = currPos.x - nx * currSize;
                      const endY = currPos.y - ny * currSize;
                      
                      return (
                        <path 
                          key={`link-${day}`}
                          d={`M ${startX} ${startY} Q ${(startX + endX)/2} ${startY}, ${endX} ${endY}`}
                          fill="none" 
                          stroke={color.main} 
                          strokeWidth="3" 
                          strokeDasharray="10,6" 
                          strokeLinecap="round" 
                          markerEnd="url(#arrow)"
                        />
                      );
                    }
                    return null;
                  })()}
                  
                  {/* 每天内部的连线 - 每段都有箭头 */}
                  {daySpots.map((spot, idx) => {
                    if (idx === 0) return null;
                    const prevSpot = daySpots[idx - 1];
                    const currSpot = spot;
                    const prevPos = getPosition(prevSpot);
                    const currPos = getPosition(currSpot);
                    const prevSize = getNodeSize(prevSpot, daySpots, idx - 1);
                    const currSize = getNodeSize(currSpot, daySpots, idx);
                    
                    // 计算方向向量
                    const dx = currPos.x - prevPos.x;
                    const dy = currPos.y - prevPos.y;
                    const dist = Math.sqrt(dx * dx + dy * dy) || 1;
                    const nx = dx / dist;
                    const ny = dy / dist;
                    
                    // 起点和终点都在圆边缘
                    const startX = prevPos.x + nx * prevSize;
                    const startY = prevPos.y + ny * prevSize;
                    const endX = currPos.x - nx * currSize;
                    const endY = currPos.y - ny * currSize;
                    
                    return (
                      <path 
                        key={`path-${day}-${idx}`}
                        d={`M ${startX} ${startY} Q ${(startX + endX)/2} ${startY}, ${endX} ${endY}`}
                        fill="none" 
                        stroke={color.main} 
                        strokeWidth="3" 
                        strokeDasharray="10,6" 
                        strokeLinecap="round" 
                        markerEnd="url(#arrow)"
                      />
                    );
                  })}
                </g>
              );
            })}

            {/* 绘制节点 */}
            {days.map((day, dayIdx) => {
              const daySpots = (spotsByDay[day] || []).sort((a, b) => a.order_index - b.order_index);
              const color = colors[dayIdx % colors.length];

              return daySpots.map((spot, idx) => {
                const pos = getPosition(spot);
                const nodeSize = getNodeSize(spot, daySpots, idx);
                const isDimmed = selectedDay !== null && selectedDay !== day;
                const isFirst = idx === 0;
                const isLast = idx === daySpots.length - 1;

                return (
                  <g 
                    key={`${day}-${idx}`} 
                    transform={`translate(${pos.x}, ${pos.y})`} 
                    opacity={isDimmed ? 0.3 : 1}
                  >
                    <circle r={nodeSize + 8} fill={color.main} opacity="0.15">
                      <animate attributeName="r" values={`${nodeSize + 6};${nodeSize + 12};${nodeSize + 6}`} dur="2.5s" repeatCount="indefinite" />
                    </circle>
                    <circle r={nodeSize} fill={color.main} stroke="#fff" strokeWidth="3"/>
                    <text textAnchor="middle" dominantBaseline="central" fill="#fff" fontSize="5" fontWeight="bold">{day}</text>
                    
                    {/* 照片图标 */}
                    {getSpotPhotos(spot.id, spot.name).length > 0 && (
                      <g transform={`translate(${nodeSize + 6}, -${nodeSize + 6})`} style={{ cursor: 'pointer' }} onClick={() => setSelectedPhoto(getSpotPhotos(spot.id, spot.name)[0])}>
                        <circle r="6" fill="#fff" stroke={color.main} strokeWidth="2"/>
                        <text textAnchor="middle" y="2" fontSize="7">📷</text>
                      </g>
                    )}
                    
                    {idx > 0 && (
                      <g transform={`translate(0, ${nodeSize + 10})`}>
                        <text textAnchor="middle" y="4" fill="#9ca3af" fontSize="4">
                          {Math.round(calculateDistance(daySpots[idx-1].latitude, daySpots[idx-1].longitude, spot.latitude, spot.longitude))}km
                        </text>
                      </g>
                    )}
                    <g transform={`translate(0, -${nodeSize + 8})`}>
                      <text textAnchor="middle" y="4" fill="#fff" fontSize="5" fontWeight="500">{spot.name.length > 18 ? spot.name.slice(0, 18) + '..' : spot.name}</text>
                    </g>
                  </g>
                );
              });
            })}
          </svg>

          <div className="mt-4 flex items-center justify-center gap-6 text-xs text-gray-400">
            <div className="flex items-center gap-2"><span>节点大小:</span><div className="flex items-center gap-1"><div className="w-3.5 h-3.5 rounded-full bg-blue-400" /><span>近</span></div><div className="flex items-center gap-1"><div className="w-7 h-7 rounded-full bg-blue-400" /><span>远</span></div></div>
          </div>

          <div className="mt-4 flex flex-wrap gap-4 justify-center">
            {days.map(day => {
              const color = colors[(day - 1) % colors.length];
              const count = (spotsByDay[day] || []).length;
              const isDimmed = selectedDay !== null && selectedDay !== day;
              return <div key={day} className={`flex items-center gap-2 px-4 py-2 bg-gray-700/50 rounded-full ${isDimmed ? 'opacity-30' : ''}`}><div className="w-3 h-3 rounded-full" style={{ backgroundColor: color.main }} /><span className="text-sm text-gray-300">第{day}天</span><span className="text-xs text-gray-500">({count}站)</span></div>;
            })}
          </div>
        </div>

        <div className="mt-6">
          <h3 className="text-sm font-medium text-gray-400 mb-3">路线详情</h3>
          <div className="space-y-4">
            {days.map(day => {
              const daySpots = (spotsByDay[day] || []).sort((a, b) => a.order_index - b.order_index);
              const color = colors[(day - 1) % colors.length];
              const isDimmed = selectedDay !== null && selectedDay !== day;
              let totalDistance = 0;
              for (let i = 1; i < daySpots.length; i++) {
                totalDistance += calculateDistance(daySpots[i-1].latitude, daySpots[i-1].longitude, daySpots[i].latitude, daySpots[i].longitude);
              }
              return (
                <div key={day} className={`bg-gray-800 rounded-xl p-4 ${isDimmed ? 'opacity-40' : ''}`}>
                  <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold text-white" style={{ backgroundColor: color.main }}>{day}</div>
                      <div><div className="font-medium">第{day}天</div><div className="text-xs text-gray-500">{daySpots.length} 个景点 · {Math.round(totalDistance)} km</div></div>
                    </div>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {daySpots.map((spot, idx) => (
                      <div key={spot.id} className="flex items-center gap-1 bg-gray-700/50 px-3 py-2 rounded-lg text-sm">
                        <span className="w-5 h-5 rounded-full flex items-center justify-center text-xs text-white" style={{ backgroundColor: color.light }}>{idx + 1}</span>
                        <span className="text-gray-300">{spot.name}</span>
                        {idx < daySpots.length - 1 && <span className="text-gray-600 ml-1">→</span>}
                      </div>
                    ))}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* 照片查看弹窗 */}
      {selectedPhoto && (
        <div 
          className="fixed inset-0 bg-black/80 flex items-center justify-center z-50"
          onClick={() => setSelectedPhoto(null)}
        >
          <div className="max-w-4xl max-h-[90vh] p-4" onClick={e => e.stopPropagation()}>
            <img 
              src={selectedPhoto.url} 
              alt={selectedPhoto.title}
              className="max-w-full max-h-[80vh] object-contain rounded-lg"
            />
            <div className="mt-4 text-center">
              <div className="text-white text-lg font-medium">{selectedPhoto.title}</div>
              {selectedPhoto.location && <div className="text-gray-400 text-sm mt-1">{selectedPhoto.location}</div>}
            </div>
            <div className="mt-4 flex justify-center gap-4">
              {photos.filter(p => p.spot_id === selectedPhoto.spot_id).map((p, idx, arr) => {
                const currIdx = arr.findIndex(x => x.id === selectedPhoto.id);
                return (
                  <button
                    key={p.id}
                    className={`px-3 py-1 rounded ${p.id === selectedPhoto.id ? 'bg-blue-500 text-white' : 'bg-gray-700 text-gray-300'}`}
                    onClick={(e) => { e.stopPropagation(); setSelectedPhoto(p); }}
                  >
                    {idx + 1}
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
