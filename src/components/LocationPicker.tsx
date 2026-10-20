"use client";

import { useEffect, useRef, useState } from "react";

const AMAP_KEY = "9b7352f8a857e05961715ee592ca9a6c";
const AMAP_SECURITY_CODE = "5cd7952e154775d411353e2b8c4cdd01";

interface SelectedLocation {
  name: string;
  latitude: number;
  longitude: number;
  address?: string;
}

type LocationSelectHandler = (location: SelectedLocation) => void;

interface AMapLngLat {
  lng: number;
  lat: number;
}

interface AMapMapOptions {
  zoom: number;
  center: [number, number];
}

interface AMapMarkerOptions {
  position: [number, number] | null;
  draggable: boolean;
}

interface AMapAutoCompleteOptions {
  city: string;
  citylimit: boolean;
  type: string;
}

interface AMapMap {
  destroy(): void;
  setCenter(position: [number, number]): void;
  setZoom(zoom: number): void;
  on(event: "click", listener: (event: { lnglat: AMapLngLat }) => void): void;
}

interface AMapMarker {
  getPosition(): AMapLngLat;
  setMap(map: AMapMap): void;
  setPosition(position: [number, number]): void;
  on(event: "dragend", listener: () => void): void;
}

interface AMapSearchTip {
  name: string;
  address?: string;
  district?: string;
  location?: AMapLngLat;
}

interface AMapSearchResult {
  tips?: AMapSearchTip[];
}

interface AMapGeocodeResult {
  regeocode?: {
    formattedAddress: string;
    pois?: Array<{ name?: string }>;
  };
}

interface AMapAutoComplete {
  search(
    keyword: string,
    callback: (status: string, result: AMapSearchResult) => void
  ): void;
}

interface AMapGeocoder {
  getAddress(
    position: [number, number],
    callback: (status: string, result: AMapGeocodeResult) => void
  ): void;
}

interface AMapNamespace {
  Map: new (container: HTMLElement, options: AMapMapOptions) => AMapMap;
  Marker: new (options: AMapMarkerOptions) => AMapMarker;
  AutoComplete: new (options: AMapAutoCompleteOptions) => AMapAutoComplete;
  Geocoder: new () => AMapGeocoder;
}

interface InitializedMap {
  map: AMapMap;
  marker: AMapMarker;
  autoComplete: AMapAutoComplete;
}

interface LocationPickerProps {
  onLocationSelect: LocationSelectHandler;
  initialLatitude?: number | null;
  initialLongitude?: number | null;
  initialName?: string;
}

declare global {
  interface Window {
    AMap?: AMapNamespace;
    _AMapSecurityConfig?: { securityJsCode: string };
  }
}

function initializeMap({
  container,
  initialLatitude,
  initialLongitude,
  initialName,
  onLocationSelect,
}: {
  container: HTMLElement;
  initialLatitude?: number | null;
  initialLongitude?: number | null;
  initialName?: string;
  onLocationSelect: LocationSelectHandler;
}): InitializedMap | null {
  const amap = window.AMap;
  if (!amap) {
    return null;
  }

  const mapInstance = new amap.Map(container, {
    zoom: 5,
    center: [121.473701, 31.230416],
  });

  const markerInstance = new amap.Marker({
    position: null,
    draggable: true,
  });

  const autoComplete = new amap.AutoComplete({
    city: "全国",
    citylimit: false,
    type: "风景名胜|商务住宅|风景名胜|交通设施",
  });

  if (initialLatitude && initialLongitude) {
    const position: [number, number] = [initialLongitude, initialLatitude];
    markerInstance.setPosition(position);
    markerInstance.setMap(mapInstance);
    mapInstance.setCenter(position);
    mapInstance.setZoom(12);
  }

  mapInstance.on("click", ({ lnglat }) => {
    const { lng, lat } = lnglat;
    markerInstance.setPosition([lng, lat]);
    markerInstance.setMap(mapInstance);

    const geocoder = new amap.Geocoder();
    geocoder.getAddress([lng, lat], (status, result) => {
      if (status === "complete" && result.regeocode) {
        const address = result.regeocode.formattedAddress;
        const name = initialName || result.regeocode.pois?.[0]?.name || address;
        onLocationSelect({ name, latitude: lat, longitude: lng, address });
      } else {
        onLocationSelect({
          name: initialName || "选中位置",
          latitude: lat,
          longitude: lng,
        });
      }
    });
  });

  markerInstance.on("dragend", () => {
    const position = markerInstance.getPosition();
    onLocationSelect({
      name: initialName || "选中位置",
      latitude: position.lat,
      longitude: position.lng,
    });
  });

  return { map: mapInstance, marker: markerInstance, autoComplete };
}

export default function LocationPicker({
  onLocationSelect,
  initialLatitude,
  initialLongitude,
  initialName,
}: LocationPickerProps) {
  const mapRef = useRef<HTMLDivElement>(null);
  const [map, setMap] = useState<AMapMap | null>(null);
  const [marker, setMarker] = useState<AMapMarker | null>(null);
  const [searchText, setSearchText] = useState("");
  const [searchResults, setSearchResults] = useState<AMapSearchTip[]>([]);
  const [showResults, setShowResults] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const autoCompleteRef = useRef<AMapAutoComplete | null>(null);
  const optionsRef = useRef({
    onLocationSelect,
    initialLatitude,
    initialLongitude,
    initialName,
  });

  useEffect(() => {
    optionsRef.current = {
      onLocationSelect,
      initialLatitude,
      initialLongitude,
      initialName,
    };
  }, [onLocationSelect, initialLatitude, initialLongitude, initialName]);

  // 加载高德地图 SDK（带安全密钥）
  useEffect(() => {
    window._AMapSecurityConfig = {
      securityJsCode: AMAP_SECURITY_CODE,
    };

    let active = true;
    let mapInstance: AMapMap | null = null;
    let script: HTMLScriptElement | null = null;

    const startMap = () => {
      if (!active || !mapRef.current) {
        return;
      }

      try {
        const initialized = initializeMap({
          container: mapRef.current,
          ...optionsRef.current,
        });

        if (!initialized) {
          setError("地图初始化失败");
          return;
        }

        mapInstance = initialized.map;
        autoCompleteRef.current = initialized.autoComplete;
        setMap(initialized.map);
        setMarker(initialized.marker);
        setLoading(false);
        setError(null);
      } catch (err) {
        console.error("地图初始化错误:", err);
        setError("地图初始化失败");
      }
    };

    const existingScript = document.querySelector<HTMLScriptElement>(
      'script[src*="webapi.amap.com"]'
    );

    if (existingScript) {
      if (window.AMap) {
        startMap();
      } else {
        existingScript.addEventListener("load", startMap, { once: true });
      }
    } else {
      script = document.createElement("script");
      script.src = `https://webapi.amap.com/maps?v=2.0&key=${AMAP_KEY}&plugin=AMap.AutoComplete,AMap.Geocoder`;
      script.async = true;
      script.onload = startMap;
      script.onerror = () => {
        setError("地图加载失败，请刷新页面重试");
      };
      document.head.appendChild(script);
    }

    return () => {
      active = false;
      existingScript?.removeEventListener("load", startMap);
      if (script) {
        script.onload = null;
        script.onerror = null;
      }
      mapInstance?.destroy();
      mapInstance = null;
      autoCompleteRef.current = null;
    };
  }, []);

  // 搜索
  const handleSearch = () => {
    const autoComplete = autoCompleteRef.current;
    if (!searchText.trim() || !autoComplete) return;

    autoComplete.search(searchText, (status, result) => {
      if (status === "complete" && result.tips) {
        const validResults = result.tips
          .filter((tip) => tip.location && tip.location.lng && tip.location.lat)
          .slice(0, 5);
        
        if (validResults.length > 0) {
          setSearchResults(validResults);
          setShowResults(true);
          setError(null);
        } else {
          alert("未找到相关地点");
        }
      } else if (status === "no_data") {
        alert("未找到相关地点");
      } else {
        console.error("搜索失败:", status, result);
        alert("搜索失败，请重试");
      }
    });
  };

  // 选择结果
  const handleSelectResult = (poi: AMapSearchTip) => {
    const { name, location, address, district } = poi;
    if (!map || !marker || !location) return;

    map.setCenter([location.lng, location.lat]);
    map.setZoom(15);
    marker.setPosition([location.lng, location.lat]);
    marker.setMap(map);
    onLocationSelect({ name, latitude: location.lat, longitude: location.lng, address: address || district });
    setShowResults(false);
    setSearchText("");
  };

  if (error) {
    return (
      <div className="p-4 bg-red-50 border border-red-200 rounded-lg text-center">
        <div className="text-red-500">{error}</div>
        <button onClick={() => window.location.reload()} className="mt-2 text-blue-500 underline">
          刷新页面
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      <div className="relative">
        <div className="flex gap-2">
          <input
            type="text"
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            onKeyDown={(e) => { e.stopPropagation(); if (e.key === "Enter") handleSearch(); }}
            onFocus={() => searchResults.length > 0 && setShowResults(true)}
            onClick={(e) => e.stopPropagation()}
            className="input-field flex-1"
            placeholder="输入地名搜索（如：浦东机场、西湖）"
            disabled={loading}
          />
          <button
            type="button"
            onClick={(e) => { e.stopPropagation(); handleSearch(); }}
            disabled={loading || !searchText.trim()}
            className="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 disabled:opacity-50"
          >
            搜索
          </button>
        </div>

        {showResults && searchResults.length > 0 && (
          <div className="absolute z-50 w-full mt-1 bg-white dark:bg-gray-800 border rounded-lg shadow-lg max-h-64 overflow-y-auto" onClick={(e) => e.stopPropagation()}>
            <div className="p-2 text-xs text-gray-400 border-b">找到 {searchResults.length} 个结果，点击选择</div>
            {searchResults.map((poi, index) => (
              <div key={index} onClick={() => handleSelectResult(poi)} className="p-3 hover:bg-blue-50 cursor-pointer border-b last:border-b-0">
                <div className="font-medium">{poi.name}</div>
                <div className="text-sm text-gray-500">{poi.address || poi.district}</div>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="relative">
        {loading && (
          <div className="absolute inset-0 bg-gray-100 flex items-center justify-center rounded-lg z-10">
            地图加载中...
          </div>
        )}
        <div ref={mapRef} className="w-full h-64 rounded-lg border bg-gray-100" />
      </div>

      <div className="text-xs text-gray-400">💡 搜索地点后点击选择，或直接在地图上点击</div>
    </div>
  );
}
