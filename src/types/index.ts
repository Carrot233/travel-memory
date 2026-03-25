// 数据类型定义

export interface User {
  id: number;
  username: string;
  role: "admin" | "user";
  created_at: string;
}

export interface Photo {
  id: number;
  title: string;
  description: string | null;
  url: string;
  location: string | null;
  taken_at: string | null;
  created_at: string;
}

export interface Route {
  id: number;
  title: string;
  description: string | null;
  cover_url: string | null;
  days: number;
  created_at: string;
  spots?: RouteSpot[];
}

export interface RouteSpot {
  id: number;
  route_id: number;
  name: string;
  description: string | null;
  latitude: number | null;
  longitude: number | null;
  day_number: number;
  order_index: number;
}

export interface Video {
  id: number;
  title: string;
  description: string | null;
  url: string;
  thumbnail_url: string | null;
  duration: number | null;
  created_at: string;
}

export interface Story {
  id: number;
  title: string;
  content: string;
  cover_url: string | null;
  location: string | null;
  created_at: string;
}

export interface Like {
  id: number;
  user_id: number;
  target_type: "photo" | "route" | "video" | "story";
  target_id: number;
  created_at: string;
}

export interface Comment {
  id: number;
  user_id: number;
  target_type: "photo" | "route" | "video" | "story";
  target_id: number;
  content: string;
  created_at: string;
  username?: string;
}
