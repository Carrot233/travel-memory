import { NextRequest, NextResponse } from "next/server";
import bcrypt from "bcryptjs";
import db, { initDatabase } from "@/lib/db";

// 初始化数据库
initDatabase();

export async function POST(request: NextRequest) {
  try {
    const { username, password } = await request.json();

    if (!username || !password) {
      return NextResponse.json(
        { error: "用户名和密码不能为空" },
        { status: 400 }
      );
    }

    const user = db.prepare("SELECT * FROM users WHERE username = ?").get(username) as any;

    if (!user) {
      return NextResponse.json(
        { error: "用户名或密码错误" },
        { status: 401 }
      );
    }

    const isValid = bcrypt.compareSync(password, user.password);

    if (!isValid) {
      return NextResponse.json(
        { error: "用户名或密码错误" },
        { status: 401 }
      );
    }

    // 设置简单的 session cookie
    const response = NextResponse.json({
      user: {
        id: user.id,
        username: user.username,
        role: user.role,
      },
    });

    // 使用简单的 base64 编码存储用户信息（生产环境应使用 JWT 或 session store）
    const sessionData = Buffer.from(
      JSON.stringify({
        id: user.id,
        username: user.username,
        role: user.role,
        exp: Date.now() + 7 * 24 * 60 * 60 * 1000, // 7天过期
      })
    ).toString("base64");

    // 注意：secure 需要配合 HTTPS 使用，HTTP 环境下设为 false
    response.cookies.set("session", sessionData, {
      httpOnly: true,
      secure: false, // 允许 HTTP 环境使用
      sameSite: "lax",
      maxAge: 7 * 24 * 60 * 60, // 7天
      path: "/",
    });

    return response;
  } catch (error) {
    console.error("登录错误:", error);
    return NextResponse.json(
      { error: "服务器错误" },
      { status: 500 }
    );
  }
}
