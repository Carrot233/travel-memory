import Link from "next/link";

export default function Footer() {
  return (
    <footer className="bg-white dark:bg-gray-900 border-t border-gray-100 dark:border-gray-800">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          {/* Brand */}
          <div className="col-span-1 md:col-span-2">
            <div className="flex items-center space-x-2 mb-4">
              <span className="text-2xl">🌍</span>
              <span className="text-xl font-bold bg-gradient-to-r from-blue-500 to-teal-400 bg-clip-text text-transparent">
                旅途记忆
              </span>
            </div>
            <p className="text-gray-500 dark:text-gray-400 max-w-md">
              记录每一段美好旅程，分享旅途中的点点滴滴。照片、路线、视频、故事，都在这里。
            </p>
          </div>

          {/* Links */}
          <div>
            <h3 className="font-semibold text-gray-900 dark:text-white mb-4">导航</h3>
            <ul className="space-y-2">
              <li><Link href="/photos" className="text-gray-500 hover:text-blue-500 transition-colors">照片</Link></li>
              <li><Link href="/routes" className="text-gray-500 hover:text-blue-500 transition-colors">路线</Link></li>
              <li><Link href="/videos" className="text-gray-500 hover:text-blue-500 transition-colors">视频</Link></li>
              <li><Link href="/stories" className="text-gray-500 hover:text-blue-500 transition-colors">故事</Link></li>
            </ul>
          </div>

          {/* Contact */}
          <div>
            <h3 className="font-semibold text-gray-900 dark:text-white mb-4">联系</h3>
            <ul className="space-y-2 text-gray-500 dark:text-gray-400">
              <li>邮箱：hello@travel.com</li>
              <li>微信：travel_memories</li>
            </ul>
          </div>
        </div>

        <div className="mt-8 pt-8 border-t border-gray-100 dark:border-gray-800 text-center text-gray-400">
          <p>© {new Date().getFullYear()} 旅途记忆. All rights reserved.</p>
        </div>
      </div>
    </footer>
  );
}
