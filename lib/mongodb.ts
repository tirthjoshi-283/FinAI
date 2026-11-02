import mongoose from "mongoose";

const MONGODB_URI = process.env.MONGODB_URI as string;

if (!MONGODB_URI) {
    throw new Error("Please define MONGODB_URI in .env.local");
}

// Use a cached connection across hot-reloads in development
let cached = (global as unknown as { __mongoose: { conn: typeof mongoose | null; promise: Promise<typeof mongoose> | null } }).__mongoose;

if (!cached) {
    cached = (global as unknown as { __mongoose: { conn: typeof mongoose | null; promise: Promise<typeof mongoose> | null } }).__mongoose = { conn: null, promise: null };
}

export async function connectMongo() {
    if (cached.conn) return cached.conn;

    if (!cached.promise) {
        cached.promise = mongoose.connect(MONGODB_URI, {
            bufferCommands: false,
            // Keep a small pool — prevents cold-reconnect delay on every request
            maxPoolSize: 5,
            // Fail fast if the server is unreachable (avoids 30s default hang)
            serverSelectionTimeoutMS: 5000,
            connectTimeoutMS: 5000,
            socketTimeoutMS: 15000,
        }).then((m) => m).catch((err) => {
            console.error("MongoDB connection failed:", err);
            cached.promise = null; // reset to allow retry
            throw err;
        });
    }

    try {
        cached.conn = await cached.promise;
    } catch (e) {
        cached.promise = null;
        throw e;
    }
    
    return cached.conn;
}
