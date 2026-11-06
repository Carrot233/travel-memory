import assert from "node:assert/strict";
import { mkdtempSync, mkdirSync, readFileSync, rmSync, symlinkSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import test from "node:test";
import { readStoredMedia } from "./media-storage.ts";
import {
  buildStoredFileName,
  hasDangerousExtension,
  isBlockedUploadPath,
  mediaResponseHeaders,
  sniffMedia,
  validateMediaUpload,
  type UploadFileLike,
} from "./media-upload.ts";

const jpegPath = path.join(process.cwd(), "public/uploads/photos/1774231770074_wox8d6.jpg");
const pngPath = path.join(process.cwd(), "public/uploads/photos/1774240494994_nfmka7.png");
const jpeg = readFileSync(jpegPath);
const png = readFileSync(pngPath);

function asFile(bytes: Buffer, name: string, type = ""): UploadFileLike {
  return {
    name,
    type,
    size: bytes.length,
    arrayBuffer: async () =>
      bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength) as ArrayBuffer,
  };
}

test("sniffs real travel photos and common media signatures", () => {
  assert.equal(sniffMedia(jpeg), "image/jpeg");
  assert.equal(sniffMedia(png), "image/png");
  assert.equal(sniffMedia(Buffer.from("GIF89aXXXX")), "image/gif");
  assert.equal(sniffMedia(Buffer.from("GIF87aXXXX")), "image/gif");

  const webp = Buffer.alloc(12);
  webp.write("RIFF", 0, "ascii");
  webp.write("WEBP", 8, "ascii");
  assert.equal(sniffMedia(webp), "image/webp");

  const mp4 = Buffer.alloc(16);
  mp4.writeUInt32BE(16, 0);
  mp4.write("ftyp", 4, "ascii");
  mp4.write("mp42", 8, "ascii");
  assert.equal(sniffMedia(mp4), "video/mp4");

  const webm = Buffer.concat([Buffer.from([0x1a, 0x45, 0xdf, 0xa3]), Buffer.from("webm")]);
  assert.equal(sniffMedia(webm), "video/webm");
  assert.equal(sniffMedia(Buffer.from("OggSrest")), "video/ogg");
  assert.equal(sniffMedia(Buffer.from("not-a-media-file")), null);
});

test("accepts a real jpeg when the extension and content agree", async () => {
  const verdict = await validateMediaUpload(asFile(jpeg, "album.JPG", "image/jpeg"), "photos");
  assert.equal(verdict.ok, true);
  if (verdict.ok) {
    assert.equal(verdict.extension, "jpg");
    assert.equal(verdict.contentType, "image/jpeg");
  }
});

test("accepts octet-stream when the bytes are a real image", async () => {
  const verdict = await validateMediaUpload(
    asFile(png, "cover.png", "application/octet-stream"),
    "thumbnails"
  );
  assert.equal(verdict.ok, true);
});

test("rejects script extensions even when a client mime claims to be an image", async () => {
  const names = ["photo.php", "photo.phtml", "photo.php.jpg", "photo.jpg.php", "shell.asp", "page.html"];
  for (const name of names) {
    const verdict = await validateMediaUpload(asFile(jpeg, name, "image/jpeg"), "photos");
    assert.equal(verdict.ok, false, name);
    if (!verdict.ok) {
      assert.equal(verdict.error, "不允许上传该类型的文件");
    }
  }
  assert.equal(hasDangerousExtension("trip.php.jpg"), true);
});

test("rejects non-media bytes that use an image name and image mime", async () => {
  const verdict = await validateMediaUpload(
    asFile(Buffer.from("not-a-media-file"), "photo.jpg", "image/jpeg"),
    "photos"
  );
  assert.equal(verdict.ok, false);
  if (!verdict.ok) {
    assert.equal(verdict.error, "文件内容与声明的媒体类型不一致");
  }
});

test("rejects a non-media client mime and mismatched image types", async () => {
  const blocked = await validateMediaUpload(asFile(jpeg, "photo.jpg", "text/html"), "photos");
  assert.equal(blocked.ok, false);

  const mismatch = await validateMediaUpload(asFile(png, "photo.jpg", "image/png"), "photos");
  assert.equal(mismatch.ok, false);
  if (!mismatch.ok) {
    assert.equal(mismatch.error, "文件内容与声明的媒体类型不一致");
  }
});

test("rejects videos in photo uploads and unknown categories stay out of the allowlist", async () => {
  const mp4 = Buffer.alloc(16);
  mp4.write("ftyp", 4, "ascii");
  mp4.write("isom", 8, "ascii");
  const verdict = await validateMediaUpload(asFile(mp4, "clip.mp4", "video/mp4"), "photos");
  assert.equal(verdict.ok, false);
  if (!verdict.ok) {
    assert.equal(verdict.error, "只支持 JPG、PNG、GIF、WEBP 格式的图片");
  }

  const video = await validateMediaUpload(asFile(mp4, "clip.mp4", "video/mp4"), "videos");
  assert.equal(video.ok, true);
});

test("enforces the caller size limit", async () => {
  const verdict = await validateMediaUpload(asFile(jpeg, "photo.jpg", "image/jpeg"), "photos", {
    imageBytes: 32,
    videoBytes: 32,
  });
  assert.equal(verdict.ok, false);
  if (!verdict.ok) {
    assert.equal(verdict.error, "文件过大");
  }
});

test("stored names are generated and contain only one allowlisted extension", () => {
  const name = buildStoredFileName("jpg");
  assert.match(name, /^\d+_[a-z0-9]+\.jpg$/);
  assert.equal(name.includes("php"), false);
  assert.throws(() => buildStoredFileName("php"));
});

test("blocks traversal and script paths before they can be served", () => {
  assert.equal(isBlockedUploadPath("/uploads/photos/1774231770074_wox8d6.jpg"), false);
  assert.equal(isBlockedUploadPath("/api/uploads/photos/1774240494994_nfmka7.png"), false);
  assert.equal(isBlockedUploadPath("/uploads/photos/shell.php"), true);
  assert.equal(isBlockedUploadPath("/uploads/photos/a.php.jpg"), true);
  assert.equal(isBlockedUploadPath("/uploads/photos/%2e%2e/%2e%2e/etc/passwd"), true);
  assert.equal(isBlockedUploadPath("/uploads/photos/..%2F..%2Fpackage.json"), true);
  assert.equal(isBlockedUploadPath("/api/uploads/photos/%00.jpg"), true);
  assert.equal(isBlockedUploadPath("/uploads/not-a-category/a.jpg"), true);
});

test("serves an existing tracked photo and refuses paths outside the upload roots", async () => {
  const located = await readStoredMedia(["photos", "1774231770074_wox8d6.jpg"]);
  assert.ok(located);
  assert.equal(located?.contentType, "image/jpeg");
  assert.equal(sniffMedia(located!.buffer), "image/jpeg");
  assert.equal(await readStoredMedia(["photos", "..", "..", "package.json"]), null);
  assert.equal(await readStoredMedia(["photos", "..%2F..%2Fpackage.json"]), null);
  assert.equal(await readStoredMedia(["../public", "file.svg"]), null);

  const headers = mediaResponseHeaders("image/jpeg", located!.fileName);
  assert.equal(headers["Content-Type"], "image/jpeg");
  assert.equal(headers["X-Content-Type-Options"], "nosniff");
  assert.match(headers["Content-Disposition"], /^inline; filename="[a-zA-Z0-9._-]+"$/);
  assert.match(headers["Content-Security-Policy"], /sandbox/);
});

test("does not follow a symlink out of the upload directory", async () => {
  const category = path.join(process.cwd(), "data", "uploads", "photos");
  mkdirSync(category, { recursive: true });
  const link = path.join(category, "linked.jpg");
  const outside = path.join(mkdtempSync(path.join(tmpdir(), "upload-outside-")), "secret.jpg");
  writeFileSync(outside, jpeg);
  symlinkSync(outside, link);
  try {
    assert.equal(await readStoredMedia(["photos", "linked.jpg"]), null);
  } finally {
    rmSync(link, { force: true });
    rmSync(path.dirname(outside), { recursive: true, force: true });
  }
});
