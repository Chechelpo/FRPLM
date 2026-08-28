export enum ChatCompletionRole {
    USER = 'user',
    ASSISTANT = 'assistant',
    SYSTEM = 'system',
}
export interface ChatCompletionMessage{
    role: ChatCompletionRole;
    content: string;
}
export interface ChatCompletionRequest{
    model: string;
    messages: ChatCompletionMessage[];
}