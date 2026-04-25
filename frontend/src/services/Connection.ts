import type { LLM_connection } from "@/types/DTOs/DTOs";

export async function fetchAllConnections(): Promise<LLM_connection[]> {
    const r = await fetch("/api/llm/get/all");
    if (!r.ok) throw new Error("Failed to fetch connections");
    return r.json();
}

export async function fetchActiveConnection(): Promise<LLM_connection> {
    const r = await fetch("/api/llm/get/active");
    if (!r.ok) throw new Error("Failed to fetch active connection");
    return r.json();
}

export async function fetchConnectionTypes(): Promise<string[]> {
    const r = await fetch("/api/llm/get/types");
    if (!r.ok) throw new Error("Failed to fetch connection types");
    return r.json();
}

export async function setActiveConnection(profileID: number): Promise<void> {
    await fetch(`/api/llm/set/active?profileID=${profileID}`, { method: "POST" });
}

export async function setConnectionType(type: string): Promise<void> {
    await fetch(`/api/llm/set/type?typeName=${type}`, { method: "POST" });
}

export async function setConnectionHost(host: string): Promise<void> {
    await fetch(`/api/llm/set/host?host=${encodeURIComponent(host)}`, { method: "POST" });
}

export async function createConnection(name: string): Promise<void> {
    await fetch(`/api/llm/new?name=${encodeURIComponent(name)}`, { method: "POST" });
}
