import { NextRequest, NextResponse } from "next/server";

// 使用 Nominatim (OpenStreetMap) 进行地理编码 - 免费服务
const NOMINATIM_BASE = "https://nominatim.openstreetmap.org";

interface GeocodeResult {
  lat: number;
  lon: number;
  display_name: string;
}

// 根据城市+景点名称获取坐标
async function geocodeSpot(spotName: string, cityName?: string): Promise<{ lat: number; lon: number } | null> {
  // 构造搜索词：优先用 "城市 景点"，如果提供了城市的话
  const searchTerms = cityName ? [`${cityName} ${spotName}`, spotName] : [spotName];
  
  for (const term of searchTerms) {
    try {
      const url = `${NOMINATIM_BASE}/search?format=json&q=${encodeURIComponent(term)}&limit=1&accept-language=zh`;
      const res = await fetch(url, {
        headers: {
          "User-Agent": "TravelSite/1.0"
        }
      });
      
      if (!res.ok) continue;
      
      const data: GeocodeResult[] = await res.json();
      if (data && data.length > 0) {
        return {
          lat: parseFloat(data[0].lat),
          lon: parseFloat(data[0].lon)
        };
      }
    } catch (error) {
      console.error(`Geocode error for "${term}":`, error);
    }
    
    // 避免请求过快
    await new Promise(resolve => setTimeout(resolve, 1000));
  }
  
  return null;
}

// 尝试从文案中提取城市名称
function extractCity(text: string): string | null {
  // 常见的城市名模式
  const cityPatterns = [
    /(?:大理|丽江|香格里拉|沙溪|昆明|西双版纳|腾冲|洱海|苍山|玉龙雪山)/,
    /(?:北京|上海|广州|深圳|成都|重庆|杭州|西安|苏州|南京)/,
    /(?:大理古城|丽江古城|双廊|喜洲|白沙|束河)/,
  ];
  
  for (const pattern of cityPatterns) {
    const match = text.match(pattern);
    if (match) {
      return match[0];
    }
  }
  
  // 尝试从标题提取
  const titleMatch = text.match(/^(.+?)(?:\d+天|行程|攻略)/);
  if (titleMatch) {
    return titleMatch[1].trim();
  }
  
  return null;
}

export async function POST(request: NextRequest) {
  try {
    const { spots, sourceText } = await request.json();
    
    if (!spots || !Array.isArray(spots)) {
      return NextResponse.json({ error: "缺少景点列表" }, { status: 400 });
    }

    // 从原文中提取城市名
    const cityName = extractCity(sourceText || "");
    
    const results = [];
    
    for (let i = 0; i < spots.length; i++) {
      const spot = spots[i];
      
      // 如果已有坐标，跳过
      if (spot.latitude && spot.longitude) {
        results.push({ ...spot, success: true, message: "已有坐标" });
        continue;
      }
      
      // 清理景点名称，移除时间前缀等
      const cleanName = spot.name
        .replace(/^\d+[\.\：\:\-]\s*/, '')
        .replace(/^\d+\s*[:：]\s*/, '')
        .replace(/^[上下午晚早晨中午傍]+\s*/, '')
        .trim();
      
      // 尝试获取坐标
      const coords = await geocodeSpot(cleanName, cityName || undefined);
      
      if (coords) {
        results.push({
          ...spot,
          latitude: coords.lat,
          longitude: coords.lon,
          success: true,
          message: "获取成功"
        });
      } else {
        results.push({
          ...spot,
          success: false,
          message: "未找到坐标"
        });
      }
    }
    
    // 统计
    const successCount = results.filter(r => r.success).length;
    
    return NextResponse.json({
      success: true,
      results,
      summary: {
        total: spots.length,
        success: successCount,
        failed: spots.length - successCount
      }
    });
  } catch (error) {
    console.error("获取坐标失败:", error);
    return NextResponse.json({ error: "获取坐标失败" }, { status: 500 });
  }
}
