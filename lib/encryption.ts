import crypto from "crypto";

const ALGORITHM = "aes-256-cbc";
const KEY_HEX = process.env.ENCRYPTION_KEY || "0".repeat(64); // 32 bytes = 64 hex chars
const KEY = Buffer.from(KEY_HEX, "hex");

export function encrypt(text: string): string {
    if (!text) return "";
    const iv = crypto.randomBytes(16);
    const cipher = crypto.createCipheriv(ALGORITHM, KEY, iv);
    const encrypted = Buffer.concat([cipher.update(text, "utf8"), cipher.final()]);
    return iv.toString("hex") + ":" + encrypted.toString("hex");
}

export function decrypt(ciphertext: string): string {
    if (!ciphertext) return "";
    try {
        const [ivHex, encHex] = ciphertext.split(":");
        const iv = Buffer.from(ivHex, "hex");
        const enc = Buffer.from(encHex, "hex");
        const decipher = crypto.createDecipheriv(ALGORITHM, KEY, iv);
        return Buffer.concat([decipher.update(enc), decipher.final()]).toString("utf8");
    } catch {
        return "";
    }
}
