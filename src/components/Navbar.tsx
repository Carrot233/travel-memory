"use client";

import Link from "next/link";
import { useState, useEffect } from "react";

export default function Navbar() {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [showMenu, setShowMenu] = useState(false);

  useEffect(() => {
    // 检查登录状态
    fetch("/api/auth/session", { credentials: "include" })
      .then((res) => res.json())
      .then((data) => setIsLoggedIn(!!data?.user))
      .catch(() => setIsLoggedIn(false));
  }, []);

  const handleLogout = async () => {
    await fetch("/api/auth/logout", { method: "POST" });
    setIsLoggedIn(false);
    window.location.href = "/";
  };

  return (
    <nav className="sticky top-0 z-50 bg-white/80 dark:bg-gray-900/80 backdrop-blur-md border-b border-gray-100 dark:border-gray-800">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          {/* Logo */}
          <Link href="/" className="flex items-center space-x-2">
            <span className="text-2xl">🌍</span>
            <span className="text-xl font-bold bg-gradient-to-r from-blue-500 to-teal-400 bg-clip-text text-transparent">
              旅途记忆
            </span>
          </Link>

          {/* Desktop Navigation */}
          <div className="hidden md:flex items-center space-x-8">
            <Link href="/" className="text-gray-600 hover:text-blue-500 transition-colors">
              首页
            </Link>
            <Link href="/photos" className="text-gray-600 hover:text-blue-500 transition-colors">
              照片
            </Link>
            <Link href="/routes" className="text-gray-600 hover:text-blue-500 transition-colors">
              路线
            </Link>
            <Link href="/videos" className="text-gray-600 hover:text-blue-500 transition-colors">
              视频
            </Link>
            <Link href="/stories" className="text-gray-600 hover:text-blue-500 transition-colors">
              故事
            </Link>
          </div>

          {/* Auth Buttons */}
          <div className="hidden md:flex items-center space-x-4">
            {isLoggedIn ? (
              <>
                <Link href="/admin" className="text-gray-600 hover:text-blue-500 transition-colors">
                  管理后台
                </Link>
                <button
                  onClick={handleLogout}
                  className="text-gray-600 hover:text-red-500 transition-colors"
                >
                  退出
                </button>
              </>
            ) : (
              <Link href="/login" className="btn-primary">
                登录
              </Link>
            )}
          </div>

          {/* Mobile Menu Button */}
          <button
            className="md:hidden p-2"
            onClick={() => setShowMenu(!showMenu)}
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              {showMenu ? (
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              ) : (
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
              )}
            </svg>
          </button>
        </div>

        {/* Mobile Menu */}
        {showMenu && (
          <div className="md:hidden py-4 border-t border-gray-100 dark:border-gray-800">
            <div className="flex flex-col space-y-4">
              <Link href="/" className="text-gray-600 hover:text-blue-500">首页</Link>
              <Link href="/photos" className="text-gray-600 hover:text-blue-500">照片</Link>
              <Link href="/routes" className="text-gray-600 hover:text-blue-500">路线</Link>
              <Link href="/videos" className="text-gray-600 hover:text-blue-500">视频</Link>
              <Link href="/stories" className="text-gray-600 hover:text-blue-500">故事</Link>
              {isLoggedIn ? (
                <>
                  <Link href="/admin" className="text-gray-600 hover:text-blue-500">管理后台</Link>
                  <button onClick={handleLogout} className="text-left text-gray-600 hover:text-red-500">退出</button>
                </>
              ) : (
                <Link href="/login" className="btn-primary text-center">登录</Link>
              )}
            </div>
          </div>
        )}
      </div>
    </nav>
  );
}
