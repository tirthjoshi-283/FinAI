import { NextRequest, NextResponse } from "next/server";
import { groqChat, GroqError } from "@/lib/groq";
import { verifyAuth } from "@/lib/verifyAuth";
import { loadAIContext } from "@/lib/aiContext";

export async function POST(req: NextRequest) {
    const uid = await verifyAuth(req);
    if (!uid) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

    if (!process.env.GROQ_API_KEY) {
        return NextResponse.json(
            { error: "GROQ_API_KEY is not configured in the environment variables." },
            { status: 500 }
        );
    }

    try {
        // Always analyse the caller's own data from the database, never a client-supplied list.
        const { transactions } = await loadAIContext(uid);
        if (transactions.length === 0) {
            return NextResponse.json({ error: "Add some transactions first so the AI has something to analyse." }, { status: 400 });
        }

const systemPrompt = `
You are a top-tier Financial Advisor AI. Analyze the following user transaction data and provide exactly 3 actionable, highly specific financial insights. Also provide a portfolio 'healthScore' from 0 to 100 based on their savings rate, spending patterns, and net flow.
CRITICAL MANDATE: You MUST use the Indian Rupee symbol (₹) for ALL monetary values. DO NOT use the dollar sign ($) under any circumstances.

Transactions:
${JSON.stringify(transactions)}

You must return your response STRICTLY as a raw JSON object (without markdown wrappers like \`\`\`json) matching this schema exactly:
{
  "healthScore": 85,
  "insightText": "A 2-sentence summary of why the score is what it is.",
  "insights": [
    {
      "id": 1,
      "type": "warning",
      "title": "Short title",
      "desc": "Detailed 2-sentence description",
      "color": "text-red-500",
      "bg": "bg-red-500/10",
      "border": "border-red-500/20"
    }
  ]
}
Valid values for type: "warning", "opportunity", "tip"
Valid values for color: "text-red-500", "text-green-500", "text-yellow-500"
Valid values for bg: "bg-red-500/10", "bg-green-500/10", "bg-yellow-500/10"
Valid values for border: "border-red-500/20", "border-green-500/20", "border-yellow-500/20"
`;

        let rawText = await groqChat(
            [{ role: "user", content: systemPrompt }],
            { json: true, temperature: 0.4 },
            "Insights"
        );

        // Strip markdown code fences if present
        if (rawText.startsWith("```json")) rawText = rawText.substring(7);
        else if (rawText.startsWith("```")) rawText = rawText.substring(3);
        if (rawText.endsWith("```")) rawText = rawText.substring(0, rawText.length - 3);

        const parsed = JSON.parse(rawText.trim());
        return NextResponse.json({ data: parsed });

    } catch (error) {
        const e = error as Error;
        console.error("[Insights] Final error:", e.message.substring(0, 300));

        if (e instanceof GroqError && e.status === 429) {
            return NextResponse.json(
                { error: "AI rate limit reached. Please wait a minute and retry." },
                { status: 429 }
            );
        }
        // Raw provider errors ("Groq 400: ...") mean nothing to users; details stay in the log.
        return NextResponse.json(
            { error: "The AI couldn't analyse your transactions right now. Please tap retry in a moment." },
            { status: 500 }
        );
    }
}
