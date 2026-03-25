import { NextRequest, NextResponse } from "next/server";

export async function GET(request: NextRequest) {
  const sessionCookie = request.cookies.get("session");

  if (!sessionCookie) {
    return NextResponse.json({ user: null });
  }

  try {
    const sessionData = JSON.parse(
      Buffer.from(sessionCookie.value, "base64").toString()
    );

    // 检查是否过期
    if (sessionData.exp < Date.now()) {
      const response = NextResponse.json({ user: null });
      response.cookies.delete("session");
      return response;
    }

    return NextResponse.json({
      user: {
        id: sessionData.id,
        username: sessionData.username,
        role: sessionData.role,
      },
    });
  } catch {
    return NextResponse.json({ user: null });
  }
}
