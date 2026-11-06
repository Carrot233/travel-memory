import { lstatSync } from "fs";
import { mkdir, readFile, writeFile } from "fs/promises";
import path from "path";
import {
  categoryExtensions,
  extensionTokens,
  isSafeFileName,
  isUploadCategory,
  mediaKindForExtension,
  type MediaKind,
  type UploadCategory,
} from "./media-upload.ts";

export interface StoredMedia {
  buffer: Buffer;
  contentType: MediaKind;
  fileName: string;
}

function regularFile(filePath: string): boolean {
  try {
    return lstatSync(filePath).isFile();
  } catch {
    return false;
  }
}

async function readIfRegular(filePath: string): Promise<Buffer | null> {
  if (!regularFile(filePath)) {
    return null;
  }
  return readFile(filePath);
}

export async function saveUpload(
  category: UploadCategory,
  fileName: string,
  buffer: Buffer
): Promise<void> {
  if (!isSafeFileName(fileName)) {
    throw new Error("invalid stored file name");
  }

  let directory: string;
  let filePath: string;
  if (category === "photos") {
    directory = path.join(process.cwd(), "data", "uploads", "photos");
    filePath = path.join(process.cwd(), "data", "uploads", "photos", fileName);
  } else if (category === "videos") {
    directory = path.join(process.cwd(), "data", "uploads", "videos");
    filePath = path.join(process.cwd(), "data", "uploads", "videos", fileName);
  } else {
    directory = path.join(process.cwd(), "data", "uploads", "thumbnails");
    filePath = path.join(process.cwd(), "data", "uploads", "thumbnails", fileName);
  }

  await mkdir(directory, { recursive: true, mode: 0o755 });
  await writeFile(filePath, buffer, { mode: 0o644 });
}

export async function readStoredMedia(segments: string[]): Promise<StoredMedia | null> {
  if (segments.length !== 2) {
    return null;
  }
  const [category, fileName] = segments;
  if (!isUploadCategory(category) || !isSafeFileName(fileName)) {
    return null;
  }
  const extension = extensionTokens(fileName)?.at(-1) ?? "";
  const contentType = mediaKindForExtension(extension);
  if (!contentType || !categoryExtensions(category).has(extension)) {
    return null;
  }

  let buffer: Buffer | null = null;
  if (category === "photos") {
    buffer =
      (await readIfRegular(path.join(process.cwd(), "data", "uploads", "photos", fileName))) ??
      (await readIfRegular(path.join(process.cwd(), "public", "uploads", "photos", fileName)));
  } else if (category === "videos") {
    buffer =
      (await readIfRegular(path.join(process.cwd(), "data", "uploads", "videos", fileName))) ??
      (await readIfRegular(path.join(process.cwd(), "public", "uploads", "videos", fileName)));
  } else {
    buffer =
      (await readIfRegular(path.join(process.cwd(), "data", "uploads", "thumbnails", fileName))) ??
      (await readIfRegular(path.join(process.cwd(), "public", "uploads", "thumbnails", fileName)));
  }

  if (!buffer) {
    return null;
  }
  return { buffer, contentType, fileName };
}
