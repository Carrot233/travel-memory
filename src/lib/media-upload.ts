export const UPLOAD_CATEGORIES = ["photos", "videos", "thumbnails"] as const;

export type UploadCategory = (typeof UPLOAD_CATEGORIES)[number];

export type MediaKind =
  | "image/jpeg"
  | "image/png"
  | "image/gif"
  | "image/webp"
  | "video/mp4"
  | "video/webm"
  | "video/ogg";

export interface UploadLimits {
  imageBytes: number;
  videoBytes: number;
}

export const DEFAULT_UPLOAD_LIMITS: UploadLimits = {
  imageBytes: 20 * 1024 * 1024,
  videoBytes: 200 * 1024 * 1024,
};

const IMAGE_EXTENSIONS = new Set(["jpg", "jpeg", "png", "gif", "webp"]);
const VIDEO_EXTENSIONS = new Set(["mp4", "webm", "ogg"]);

const EXTENSION_KIND: Record<string, MediaKind> = {
  jpg: "image/jpeg",
  jpeg: "image/jpeg",
  png: "image/png",
  gif: "image/gif",
  webp: "image/webp",
  mp4: "video/mp4",
  webm: "video/webm",
  ogg: "video/ogg",
};

const CLIENT_MIME_KIND: Record<string, MediaKind> = {
  "image/jpeg": "image/jpeg",
  "image/jpg": "image/jpeg",
  "image/pjpeg": "image/jpeg",
  "image/png": "image/png",
  "image/gif": "image/gif",
  "image/webp": "image/webp",
  "video/mp4": "video/mp4",
  "video/webm": "video/webm",
  "video/ogg": "video/ogg",
  "application/ogg": "video/ogg",
};

const DANGEROUS_EXTENSIONS = new Set([
  "php",
  "php3",
  "php4",
  "php5",
  "php7",
  "php8",
  "phtml",
  "phar",
  "pht",
  "phps",
  "cgi",
  "pl",
  "pm",
  "asp",
  "aspx",
  "asa",
  "cer",
  "asax",
  "jsp",
  "jspx",
  "js",
  "mjs",
  "cjs",
  "jsx",
  "ts",
  "tsx",
  "html",
  "htm",
  "xhtml",
  "shtml",
  "svg",
  "svgz",
  "sh",
  "bash",
  "zsh",
  "ksh",
  "bat",
  "cmd",
  "exe",
  "dll",
  "so",
  "com",
  "scr",
  "vbs",
  "ps1",
  "py",
  "rb",
  "jar",
  "war",
  "wasm",
  "hta",
  "inc",
  "ini",
  "env",
  "config",
  "htaccess",
]);

const SAFE_SEGMENT = /^[a-zA-Z0-9][a-zA-Z0-9._-]{0,127}$/;

const MESSAGES = {
  image: "只支持 JPG、PNG、GIF、WEBP 格式的图片",
  video: "只支持 MP4、WEBM、OGG 格式的视频",
  mismatch: "文件内容与声明的媒体类型不一致",
  blocked: "不允许上传该类型的文件",
  tooBig: "文件过大",
  empty: "文件为空",
  category: "不支持的上传类型",
};

export interface UploadFileLike {
  name: string;
  type: string;
  size: number;
  arrayBuffer: () => Promise<ArrayBuffer>;
}

export interface AcceptedUpload {
  ok: true;
  buffer: Buffer;
  extension: string;
  contentType: MediaKind;
  category: UploadCategory;
}

export interface RejectedUpload {
  ok: false;
  error: string;
}

export type UploadValidation = AcceptedUpload | RejectedUpload;

export function isUploadCategory(value: string): value is UploadCategory {
  return (UPLOAD_CATEGORIES as readonly string[]).includes(value);
}

export function categoryExtensions(category: UploadCategory): Set<string> {
  return category === "videos" ? VIDEO_EXTENSIONS : IMAGE_EXTENSIONS;
}

export function extensionTokens(fileName: string): string[] | null {
  const base = fileName.split(/[/\\]/).pop() ?? "";
  if (base.includes("\0")) {
    return null;
  }
  return base
    .split(".")
    .slice(1)
    .map((part) => part.toLowerCase().replace(/[^a-z0-9]/g, ""))
    .filter((part) => part.length > 0);
}

export function hasDangerousExtension(fileName: string): boolean {
  const tokens = extensionTokens(fileName);
  if (!tokens) {
    return true;
  }
  return tokens.some((token) => DANGEROUS_EXTENSIONS.has(token));
}

export function sniffMedia(buffer: Buffer): MediaKind | null {
  if (buffer.length >= 3 && buffer[0] === 0xff && buffer[1] === 0xd8 && buffer[2] === 0xff) {
    return "image/jpeg";
  }
  if (
    buffer.length >= 8 &&
    buffer[0] === 0x89 &&
    buffer[1] === 0x50 &&
    buffer[2] === 0x4e &&
    buffer[3] === 0x47 &&
    buffer[4] === 0x0d &&
    buffer[5] === 0x0a &&
    buffer[6] === 0x1a &&
    buffer[7] === 0x0a
  ) {
    return "image/png";
  }
  if (buffer.length >= 6) {
    const gif = buffer.subarray(0, 6).toString("ascii");
    if (gif === "GIF87a" || gif === "GIF89a") {
      return "image/gif";
    }
  }
  if (buffer.length >= 12) {
    const riff = buffer.subarray(0, 4).toString("ascii");
    const webp = buffer.subarray(8, 12).toString("ascii");
    if (riff === "RIFF" && webp === "WEBP") {
      return "image/webp";
    }
  }
  if (buffer.length >= 4 && buffer.subarray(0, 4).toString("ascii") === "OggS") {
    return "video/ogg";
  }
  if (isWebm(buffer)) {
    return "video/webm";
  }
  if (isMp4(buffer)) {
    return "video/mp4";
  }
  return null;
}

function isMp4(buffer: Buffer): boolean {
  if (buffer.length < 12) {
    return false;
  }
  if (buffer.subarray(4, 8).toString("ascii") !== "ftyp") {
    return false;
  }
  const brand = buffer.subarray(8, 12).toString("ascii");
  return /^[a-zA-Z0-9 ]{4}$/.test(brand);
}

function isWebm(buffer: Buffer): boolean {
  if (
    buffer.length < 4 ||
    buffer[0] !== 0x1a ||
    buffer[1] !== 0x45 ||
    buffer[2] !== 0xdf ||
    buffer[3] !== 0xa3
  ) {
    return false;
  }
  const probe = buffer.subarray(0, Math.min(buffer.length, 4096)).toString("latin1");
  return probe.includes("webm");
}

function limitFor(category: UploadCategory, limits: UploadLimits): number {
  return category === "videos" ? limits.videoBytes : limits.imageBytes;
}

function formatMessage(category: UploadCategory): string {
  return category === "videos" ? MESSAGES.video : MESSAGES.image;
}

function clientMimeAgrees(mime: string, detected: MediaKind): "ok" | "mismatch" | "blocked" {
  const normalized = mime.split(";")[0]?.trim().toLowerCase() ?? "";
  if (!normalized || normalized === "application/octet-stream") {
    return "ok";
  }
  const mapped = CLIENT_MIME_KIND[normalized];
  if (!mapped) {
    return "blocked";
  }
  return mapped === detected ? "ok" : "mismatch";
}

export async function validateMediaUpload(
  file: UploadFileLike,
  category: UploadCategory,
  limits: UploadLimits = DEFAULT_UPLOAD_LIMITS
): Promise<UploadValidation> {
  if (!isUploadCategory(category)) {
    return { ok: false, error: MESSAGES.category };
  }
  if (!file || typeof file.arrayBuffer !== "function") {
    return { ok: false, error: "未找到文件" };
  }
  if (file.size <= 0) {
    return { ok: false, error: MESSAGES.empty };
  }
  if (file.size > limitFor(category, limits)) {
    return { ok: false, error: MESSAGES.tooBig };
  }

  const tokens = extensionTokens(file.name || "");
  if (!tokens || tokens.length === 0) {
    return { ok: false, error: formatMessage(category) };
  }
  if (tokens.some((token) => DANGEROUS_EXTENSIONS.has(token))) {
    return { ok: false, error: MESSAGES.blocked };
  }

  const extension = tokens[tokens.length - 1];
  const expected = EXTENSION_KIND[extension];
  if (!expected || !categoryExtensions(category).has(extension)) {
    return { ok: false, error: formatMessage(category) };
  }

  const buffer = Buffer.from(await file.arrayBuffer());
  if (buffer.length === 0) {
    return { ok: false, error: MESSAGES.empty };
  }
  if (buffer.length > limitFor(category, limits)) {
    return { ok: false, error: MESSAGES.tooBig };
  }

  const detected = sniffMedia(buffer);
  if (!detected || detected !== expected) {
    return { ok: false, error: MESSAGES.mismatch };
  }

  const mimeCheck = clientMimeAgrees(file.type || "", detected);
  if (mimeCheck === "blocked") {
    return { ok: false, error: MESSAGES.blocked };
  }
  if (mimeCheck === "mismatch") {
    return { ok: false, error: MESSAGES.mismatch };
  }

  return {
    ok: true,
    buffer,
    extension,
    contentType: detected,
    category,
  };
}

export function buildStoredFileName(extension: string): string {
  const ext = extension.toLowerCase().replace(/[^a-z0-9]/g, "");
  if (!EXTENSION_KIND[ext]) {
    throw new Error("invalid media extension");
  }
  const random = Math.random().toString(36).slice(2, 10);
  return `${Date.now()}_${random}.${ext}`;
}

export function mediaResponseHeaders(contentType: MediaKind, fileName: string): Record<string, string> {
  const safeName = fileName.replace(/[^a-zA-Z0-9._-]/g, "") || "media";
  return {
    "Content-Type": contentType,
    "Content-Disposition": `inline; filename="${safeName}"`,
    "X-Content-Type-Options": "nosniff",
    "Content-Security-Policy": "default-src 'none'; sandbox",
    "Cache-Control": "public, max-age=31536000, immutable",
  };
}

export function isSafeFileName(fileName: string): boolean {
  if (!SAFE_SEGMENT.test(fileName) || fileName.includes("..")) {
    return false;
  }
  const tokens = extensionTokens(fileName);
  if (!tokens || tokens.length === 0) {
    return false;
  }
  if (tokens.some((token) => DANGEROUS_EXTENSIONS.has(token))) {
    return false;
  }
  const extension = tokens[tokens.length - 1];
  return Boolean(EXTENSION_KIND[extension]);
}

export function mediaKindForExtension(extension: string): MediaKind | null {
  return EXTENSION_KIND[extension] ?? null;
}

export function isBlockedUploadPath(pathname: string): boolean {
  let decoded = pathname;
  try {
    decoded = decodeURIComponent(pathname);
  } catch {
    return true;
  }
  if (decoded.includes("\0") || decoded.includes("\\") || decoded.includes("..")) {
    return true;
  }

  const segments = decoded.split("/").filter((part) => part.length > 0);
  const rest = segments[0] === "api" ? segments.slice(1) : segments;
  if (rest[0] !== "uploads") {
    return false;
  }
  if (rest.length !== 3) {
    return true;
  }
  const category = rest[1];
  const fileName = rest[2];
  if (!isUploadCategory(category) || !isSafeFileName(fileName)) {
    return true;
  }
  const extension = extensionTokens(fileName)?.at(-1) ?? "";
  return !categoryExtensions(category).has(extension);
}
