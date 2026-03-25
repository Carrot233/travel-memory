"use client";

import { useState, useEffect } from "react";
import Link from "next/link";

export default function CTASection() {
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  useEffect(() => {
    fetch("/api/auth/session", { credentials: "include" })
      .then((res) => res.json())
      .then((data) => setIsLoggedIn(!!data?.user))
      .catch(() => setIsLoggedIn(false));
  }, []);

  if (isLoggedIn) {
    // 已登录，显示管理入口
    return (
      <section className="py-20 bg-gradient-to-r from-blue-500 to-teal-400 text-white">
        <div className="max-w-4xl mx-auto px-4 text-center">
          <h2 className="text-3xl md:text-4xl font-bold mb-6">
            欢迎回来！
          </h2>
          <p className="text-xl opacity-90 mb-8">
            前往管理后台添加更多精彩内容
          </p>
          <Link href="/admin" className="btn-primary bg-white text-blue-600 hover:bg-gray-100">
            进入管理后台
          </Link>
        </div>
      </section>
    );
  }

  // 未登录，显示登录引导
  return (
    <section className="py-20 bg-gradient-to-r from-blue-500 to-teal-400 text-white">
      <div className="max-w-4xl mx-auto px-4 text-center">
        <h2 className="text-3xl md:text-4xl font-bold mb-6">
          开始探索精彩旅程
        </h2>
        <p className="text-xl opacity-90 mb-8">
          登录后即可点赞和评论，与我们互动
        </p>
        <Link href="/login" className="btn-primary bg-white text-blue-600 hover:bg-gray-100">
          立即登录
        </Link>
      </div>
    </section>
  );
}
